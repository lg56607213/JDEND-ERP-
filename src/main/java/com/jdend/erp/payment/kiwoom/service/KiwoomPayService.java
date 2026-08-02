package com.jdend.erp.payment.kiwoom.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdend.erp.config.KiwoomPayProperties;
import com.jdend.erp.payment.kiwoom.dto.KiwoomPayReadyRequest;
import com.jdend.erp.payment.kiwoom.dto.KiwoomPayReadyResponse;
import com.jdend.erp.payment.kiwoom.entity.KiwoomPayment;
import com.jdend.erp.payment.kiwoom.repository.KiwoomPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KiwoomPayService {

    private static final String RESULT_PAGE_URL =
            "https://rentcarerp.com/pages/payment/kiwoom_result.html";

    private final KiwoomPaymentRepository repository;
    private final KiwoomPayProperties properties;
    private final ObjectMapper objectMapper;

    // -------------------------------------------------------------------------
    // 결제 준비 (1단계 해시 요청 + PENDING 레코드 저장)
    // -------------------------------------------------------------------------

    /**
     * 프론트에서 카드결제 버튼 클릭 시 호출.
     * 1) 주문번호 생성
     * 2) 키움페이 해시 API 호출 → KIWOOM_ENC 수령
     * 3) KiwoomPayment PENDING 레코드 저장
     * 4) 결제창 진입 파라미터 반환
     */
    @Transactional
    public KiwoomPayReadyResponse preparePayment(KiwoomPayReadyRequest req) {
        String orderId = generateOrderId(req.getContractNumber());

        // 1단계: 해시 요청
        String kiwoomEnc = requestHash("CARD", "P", orderId, req.getAmount());

        // PENDING 레코드 저장 (중복 orderId 방지: UNIQUE 제약)
        KiwoomPayment payment = KiwoomPayment.builder()
                .orderId(orderId)
                .contractNumber(req.getContractNumber())
                .amount(req.getAmount())
                .paymethod("CARD")
                .status("PENDING")
                .build();
        repository.save(payment);

        // | 문자 제거 (키움페이 파이프라인 구분자 충돌 방지)
        String productName = req.getProductName() != null
                ? req.getProductName().replace("|", "") : "렌트료";

        // 결제창 파라미터 맵 구성
        Map<String, String> params = new LinkedHashMap<>();
        params.put("PAYMETHOD",       "CARD");
        params.put("TYPE",            "P");
        params.put("CPID",            properties.getCpid());
        params.put("ORDERNO",         orderId);
        params.put("AMOUNT",          String.valueOf(req.getAmount()));
        params.put("PRODUCTTYPE",     "2");       // 실물
        params.put("PRODUCTNAME",     productName);
        params.put("PRODUCTCODE",     req.getContractNumber());
        params.put("TAXFREECD",       "00");      // 과세
        params.put("KIWOOM_ENC",      kiwoomEnc);
        params.put("HOMEURL",         RESULT_PAGE_URL);
        params.put("FAILURL",         RESULT_PAGE_URL);
        params.put("CLOSEURL",        RESULT_PAGE_URL);
        params.put("DIRECTRESULTFLAG","Y");
        params.put("RESERVEDINDEX1",  orderId);   // 통지/결과 페이지에서 주문 추적용

        return KiwoomPayReadyResponse.builder()
                .payUrl(properties.getPayUrl())
                .params(params)
                .build();
    }

    // -------------------------------------------------------------------------
    // 통지 처리 (키움페이 → 서버)
    // -------------------------------------------------------------------------

    /**
     * 키움페이 서버가 POST /api/payments/kiwoom/notify 로 보내는 결제 완료 통지 처리.
     * 재통지(3분 간격, 1시간)가 오므로 DAOUTRX 중복 체크로 멱등성 보장.
     *
     * @param params 키움페이가 전송한 폼 파라미터 전체
     */
    @Transactional
    public void processNotify(Map<String, String> params) {
        String resultCode = params.getOrDefault("RESULTCODE", "");
        String daoutrx   = params.getOrDefault("DAOUTRX", "");
        // RESERVEDINDEX1 → 우리가 넣은 orderId, 없으면 ORDERNO fallback
        String orderId    = params.getOrDefault("RESERVEDINDEX1",
                            params.getOrDefault("ORDERNO", ""));
        String amountStr  = params.getOrDefault("AMOUNT", "0");

        // 결제 실패 통지: 상태만 FAIL로 업데이트하고 종료
        if (!"0000".equals(resultCode)) {
            log.warn("[KiwoomPay] 결제 실패 통지 수신: RESULTCODE={}, orderId={}", resultCode, orderId);
            repository.findByOrderId(orderId).ifPresent(p -> {
                p.setStatus("FAIL");
                p.setRawNotifyData(safeJson(params));
                repository.save(p);
            });
            return;
        }

        // 중복 통지 방지 (DAOUTRX 이미 처리됨)
        if (repository.existsByDaoutrx(daoutrx)) {
            log.info("[KiwoomPay] 중복 통지 무시: DAOUTRX={}", daoutrx);
            return;
        }

        // 주문 레코드 조회
        KiwoomPayment payment = repository.findByOrderId(orderId)
                .orElseGet(() -> {
                    // RESERVEDINDEX1 없이 온 경우 ORDERNO로 재시도
                    String orderNoFallback = params.getOrDefault("ORDERNO", "");
                    return repository.findByOrderId(orderNoFallback).orElse(null);
                });

        if (payment == null) {
            log.error("[KiwoomPay] 주문 레코드 없음: orderId={}, DAOUTRX={}", orderId, daoutrx);
            return;
        }

        // 금액 검증 (위변조 방지)
        long notifyAmount;
        try {
            notifyAmount = Long.parseLong(amountStr.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            log.error("[KiwoomPay] AMOUNT 파싱 실패: {}", amountStr);
            notifyAmount = -1L;
        }

        if (!payment.getAmount().equals(notifyAmount)) {
            log.error("[KiwoomPay] 금액 불일치! orderId={} 저장금액={} 통지금액={}",
                    payment.getOrderId(), payment.getAmount(), notifyAmount);
            payment.setStatus("FAIL");
            payment.setRawNotifyData(safeJson(params));
            repository.save(payment);
            return;
        }

        // 성공 처리
        payment.setStatus("SUCCESS");
        payment.setDaoutrx(daoutrx);
        payment.setAuthno(params.getOrDefault("AUTHNO", ""));
        payment.setCardCode(params.getOrDefault("CARDCODE", ""));
        payment.setCardName(params.getOrDefault("CARDNAME", ""));
        payment.setCardNo(params.getOrDefault("CARDNO", ""));
        payment.setInstallMonth(params.getOrDefault("INSTALLMONTH", ""));
        payment.setSettdate(params.getOrDefault("SETTDATE", ""));
        payment.setRawNotifyData(safeJson(params));
        repository.save(payment);

        log.info("[KiwoomPay] 결제 성공 처리 완료: orderId={}, DAOUTRX={}, 금액={}",
                payment.getOrderId(), daoutrx, notifyAmount);
    }

    // -------------------------------------------------------------------------
    // 결제 결과 조회
    // -------------------------------------------------------------------------

    /**
     * 결제 완료 페이지에서 최종 상태 확인용.
     *
     * @return status (PENDING / SUCCESS / FAIL / CANCEL / NOT_FOUND)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getResult(String orderId) {
        return repository.findByOrderId(orderId)
                .map(p -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("status",         p.getStatus());
                    m.put("orderId",        p.getOrderId());
                    m.put("amount",         p.getAmount());
                    m.put("contractNumber", p.getContractNumber() != null ? p.getContractNumber() : "");
                    m.put("cardName",       p.getCardName() != null ? p.getCardName() : "");
                    m.put("authno",         p.getAuthno() != null ? p.getAuthno() : "");
                    return m;
                })
                .orElseGet(() -> Map.of("status", "NOT_FOUND"));
    }

    // -------------------------------------------------------------------------
    // 1단계: 키움페이 Hash 요청 (HttpURLConnection)
    // -------------------------------------------------------------------------

    /**
     * 키움페이 해시 API 호출.
     * POST {hashUrl} Body: {"PAYMETHOD":..,"TYPE":..,"CPID":..,"ORDERNO":..,"AMOUNT":..}
     *
     * @return KIWOOM_ENC (해시값)
     * @throws RuntimeException RESULTCODE != "0000" 또는 네트워크 오류
     */
    public String requestHash(String paymethod, String type, String orderId, long amount) {
        try {
            // 요청 바디 구성
            Map<String, String> body = new LinkedHashMap<>();
            body.put("PAYMETHOD", paymethod);
            body.put("TYPE",      type);
            body.put("CPID",      properties.getCpid());
            body.put("ORDERNO",   orderId);
            body.put("AMOUNT",    String.valueOf(amount));
            String jsonBody = objectMapper.writeValueAsString(body);

            log.debug("[KiwoomPay] 해시 요청 → {}", jsonBody);

            // HTTP 연결
            URL url = new URL(properties.getHashUrl());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(15_000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            int httpStatus = conn.getResponseCode();
            InputStream is = (httpStatus < 400) ? conn.getInputStream() : conn.getErrorStream();
            String responseBody;
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                responseBody = br.lines().collect(Collectors.joining());
            }

            log.debug("[KiwoomPay] 해시 응답 ← {}", responseBody);

            Map<String, Object> resp = objectMapper.readValue(
                    responseBody, new TypeReference<>() {});

            String resultCode = String.valueOf(resp.getOrDefault("RESULTCODE", ""));
            if (!"0000".equals(resultCode)) {
                String msg = String.valueOf(resp.getOrDefault("RESULTMSG", "알 수 없는 오류"));
                throw new RuntimeException("키움페이 해시 요청 실패: RESULTCODE=" + resultCode + ", " + msg);
            }

            return String.valueOf(resp.get("KIWOOM_ENC"));

        } catch (IOException e) {
            throw new RuntimeException("키움페이 해시 API 통신 오류: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // 주문번호 생성
    // -------------------------------------------------------------------------

    /**
     * 주문번호 형식: KW + yyyyMMdd + 계약번호 끝 4자리 + 3자리 랜덤숫자.
     * 예: KW20260724000100112
     */
    public String generateOrderId(String contractNumber) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        String suffix;
        if (contractNumber != null && contractNumber.length() >= 4) {
            suffix = contractNumber.substring(contractNumber.length() - 4);
        } else if (contractNumber != null) {
            suffix = String.format("%4s", contractNumber).replace(' ', '0');
        } else {
            suffix = "0000";
        }

        String rand = String.format("%03d", new Random().nextInt(1000));
        return "KW" + date + suffix + rand;
    }

    // -------------------------------------------------------------------------
    // 내부 유틸
    // -------------------------------------------------------------------------

    private String safeJson(Map<String, String> params) {
        try {
            return objectMapper.writeValueAsString(params);
        } catch (Exception e) {
            return params.toString();
        }
    }
}
