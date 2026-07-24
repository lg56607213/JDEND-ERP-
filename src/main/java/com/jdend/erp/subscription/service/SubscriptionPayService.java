package com.jdend.erp.subscription.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdend.erp.config.KiwoomPayProperties;
import com.jdend.erp.payment.kiwoom.service.KiwoomPayService;
import com.jdend.erp.subscription.dto.SubscriptionPayReadyRequest;
import com.jdend.erp.subscription.dto.SubscriptionPayReadyResponse;
import com.jdend.erp.subscription.entity.SubscriptionPayment;
import com.jdend.erp.subscription.repository.SubscriptionPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 구독 결제 서비스.
 * - 결제 준비 (ready): 키움페이 해시 요청 + PENDING 레코드 저장 + 결제창 파라미터 반환
 * - 결제 통지 (notify): 키움페이 서버가 POST → 금액검증 + SUCCESS 업데이트 + 이메일 발송
 * - 취소 (cancel): 2단계 키움페이 취소 API 호출 → CANCEL 업데이트
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionPayService {

    private static final String RESULT_PAGE_URL = "https://jdend.co.kr/payment-result.html";

    private final SubscriptionPaymentRepository repository;
    private final KiwoomPayProperties properties;
    private final KiwoomPayService kiwoomPayService;
    private final SubscriptionEmailService emailService;
    private final ObjectMapper objectMapper;

    // ─────────────────────────────────────────────────────────
    // 결제 준비
    // ─────────────────────────────────────────────────────────

    @Transactional
    public SubscriptionPayReadyResponse ready(SubscriptionPayReadyRequest req) {
        String orderId = generateOrderId();
        String type = "M".equals(req.getType()) ? "M" : "P";

        // 1단계: 키움페이 해시(KIWOOM_ENC) 요청
        String kiwoomEnc = kiwoomPayService.requestHash("CARD", type, orderId, req.getAmount());

        // PENDING 레코드 저장
        SubscriptionPayment payment = SubscriptionPayment.builder()
                .orderId(orderId)
                .applicantName(req.getApplicantName())
                .companyName(req.getCompanyName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .planName(req.getPlanName())
                .amount(req.getAmount())
                .status("PENDING")
                .accountCreated(false)
                .build();
        repository.save(payment);

        String productName = "JDEND ERP " + req.getPlanName() + " 이용료";

        Map<String, String> params = new LinkedHashMap<>();
        params.put("PAYMETHOD",        "CARD");
        params.put("TYPE",             type);
        params.put("CPID",             properties.getCpid());
        params.put("ORDERNO",          orderId);
        params.put("AMOUNT",           String.valueOf(req.getAmount()));
        params.put("PRODUCTTYPE",      "2");
        params.put("PRODUCTNAME",      productName);
        params.put("PRODUCTCODE",      "ERP001");
        params.put("TAXFREECD",        "00");
        params.put("KIWOOM_ENC",       kiwoomEnc);
        params.put("DIRECTRESULTFLAG", "Y");
        params.put("HOMEURL",          RESULT_PAGE_URL);
        params.put("FAILURL",          RESULT_PAGE_URL);
        params.put("CLOSEURL",         RESULT_PAGE_URL);
        params.put("RESERVEDINDEX1",   orderId);
        params.put("USERNAME",         nvl(req.getApplicantName()));
        params.put("EMAIL",            nvl(req.getEmail()));

        return SubscriptionPayReadyResponse.builder()
                .payUrl(properties.getPayUrl())
                .params(params)
                .orderId(orderId)
                .build();
    }

    // ─────────────────────────────────────────────────────────
    // 결제 완료 통지 처리
    // ─────────────────────────────────────────────────────────

    @Transactional
    public void notify(Map<String, String> params) {
        String resultCode = params.getOrDefault("RESULTCODE", "");
        String daoutrx   = params.getOrDefault("DAOUTRX", "");
        String orderId   = params.getOrDefault("RESERVEDINDEX1",
                           params.getOrDefault("ORDERNO", ""));
        String amountStr = params.getOrDefault("AMOUNT", "0");

        // 결제 실패 통지
        if (!"0000".equals(resultCode)) {
            log.warn("[SubscriptionPay] 결제 실패 통지: RESULTCODE={}, orderId={}", resultCode, orderId);
            repository.findByOrderId(orderId).ifPresent(p -> {
                p.setStatus("FAIL");
                repository.save(p);
            });
            return;
        }

        // 중복 통지 방지
        if (repository.existsByDaoutrx(daoutrx)) {
            log.info("[SubscriptionPay] 중복 통지 무시: DAOUTRX={}", daoutrx);
            return;
        }

        // 주문 조회
        SubscriptionPayment payment = repository.findByOrderId(orderId).orElse(null);
        if (payment == null) {
            log.error("[SubscriptionPay] 주문 레코드 없음: orderId={}", orderId);
            return;
        }

        // 금액 검증 (위변조 방지)
        long notifyAmount;
        try {
            notifyAmount = Long.parseLong(amountStr.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            log.error("[SubscriptionPay] AMOUNT 파싱 실패: {}", amountStr);
            payment.setStatus("FAIL");
            repository.save(payment);
            return;
        }

        if (!payment.getAmount().equals(notifyAmount)) {
            log.error("[SubscriptionPay] 금액 불일치: orderId={} 저장={} 통지={}",
                    orderId, payment.getAmount(), notifyAmount);
            payment.setStatus("FAIL");
            repository.save(payment);
            return;
        }

        // 성공 처리
        payment.setStatus("SUCCESS");
        payment.setDaoutrx(daoutrx);
        payment.setAuthno(params.getOrDefault("AUTHNO", ""));
        payment.setCardName(params.getOrDefault("CARDNAME", ""));
        repository.save(payment);

        log.info("[SubscriptionPay] 결제 성공: orderId={}, DAOUTRX={}, 금액={}",
                orderId, daoutrx, notifyAmount);

        // 관리자 이메일 알림 (실패해도 결제 처리 정상)
        emailService.sendPaymentCompleteNotice(payment);
    }

    // ─────────────────────────────────────────────────────────
    // 키움페이 통합취소 (2단계)
    // ─────────────────────────────────────────────────────────

    @Transactional
    public void cancelPayment(Long id, String reason) {
        SubscriptionPayment payment = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 결제 ID: " + id));

        if (!"SUCCESS".equals(payment.getStatus())) {
            throw new IllegalStateException("결제 완료(SUCCESS) 상태만 취소할 수 있습니다. 현재 상태: " + payment.getStatus());
        }
        if (payment.getDaoutrx() == null || payment.getDaoutrx().isBlank()) {
            throw new IllegalStateException("키움페이 거래번호(DAOUTRX)가 없습니다.");
        }

        String cancelReason = (reason != null && !reason.isBlank()) ? reason : "구독취소";

        // ── 1단계: Ready 요청 ───────────────────────────
        Map<String, Object> readyBody = new LinkedHashMap<>();
        readyBody.put("CPID",        properties.getCpid());
        readyBody.put("PAYMETHOD",   "CARD");
        readyBody.put("CANCELREQ",   "Y");

        Map<String, Object> readyResp = callCancelApi(properties.getCancelUrl(), readyBody);
        String returnUrl = String.valueOf(readyResp.get("RETURNURL"));
        String token     = String.valueOf(readyResp.get("TOKEN"));

        if (returnUrl == null || "null".equals(returnUrl) || returnUrl.isBlank()) {
            throw new RuntimeException("취소 1단계 응답에 RETURNURL 없음: " + readyResp);
        }

        // ── 2단계: 취소 실행 ────────────────────────────
        Map<String, Object> cancelBody = new LinkedHashMap<>();
        cancelBody.put("CPID",         properties.getCpid());
        cancelBody.put("TRXID",        payment.getDaoutrx());
        cancelBody.put("AMOUNT",       String.valueOf(payment.getAmount()));  // 반드시 String
        cancelBody.put("CANCELREASON", cancelReason);

        Map<String, Object> cancelResp = callCancelApiWithToken(returnUrl, token, cancelBody);
        String resultCode = String.valueOf(cancelResp.getOrDefault("RESULTCODE", ""));

        if (!"0000".equals(resultCode)) {
            String errMsg = String.valueOf(cancelResp.getOrDefault("ERRORMESSAGE", "알 수 없는 오류"));
            throw new RuntimeException("키움페이 취소 실패: RESULTCODE=" + resultCode + ", " + errMsg);
        }

        // 취소 성공
        payment.setStatus("CANCEL");
        repository.save(payment);
        log.info("[SubscriptionPay] 취소 성공: id={}, DAOUTRX={}, 금액={}", id, payment.getDaoutrx(), payment.getAmount());
    }

    // ─────────────────────────────────────────────────────────
    // 조회
    // ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> getResult(String orderId) {
        return repository.findByOrderId(orderId)
                .map(p -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("status",      p.getStatus());
                    m.put("orderId",     p.getOrderId());
                    m.put("companyName", nvl(p.getCompanyName()));
                    m.put("planName",    nvl(p.getPlanName()));
                    m.put("amount",      p.getAmount());
                    return m;
                })
                .orElseGet(() -> Map.of("status", "NOT_FOUND"));
    }

    @Transactional(readOnly = true)
    public List<SubscriptionPayment> listAll() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<SubscriptionPayment> listByStatus(String status) {
        return repository.findByStatusOrderByCreatedAtDesc(status);
    }

    // ─────────────────────────────────────────────────────────
    // 관리자 액션
    // ─────────────────────────────────────────────────────────

    @Transactional
    public void markAccountCreated(Long id) {
        repository.findById(id).ifPresent(p -> {
            p.setAccountCreated(true);
            repository.save(p);
        });
    }

    @Transactional
    public void saveMemo(Long id, String memo) {
        repository.findById(id).ifPresent(p -> {
            p.setMemo(memo);
            repository.save(p);
        });
    }

    // ─────────────────────────────────────────────────────────
    // 내부 유틸
    // ─────────────────────────────────────────────────────────

    /** 주문번호 생성: SUB + YYYYMMDD + 5자리 랜덤숫자 (예: SUB2026072412345) */
    private String generateOrderId() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String rand = String.format("%05d", new Random().nextInt(100000));
        return "SUB" + date + rand;
    }

    /** 키움페이 취소 API 호출 (Authorization 헤더 포함, 응답 EUC-KR) */
    private Map<String, Object> callCancelApi(String apiUrl, Map<String, Object> body) {
        return callHttp(apiUrl, properties.getAuthorization(), null, body);
    }

    private Map<String, Object> callCancelApiWithToken(String apiUrl, String token, Map<String, Object> body) {
        return callHttp(apiUrl, properties.getAuthorization(), token, body);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> callHttp(String apiUrl, String authorization, String token, Map<String, Object> body) {
        try {
            String jsonBody = objectMapper.writeValueAsString(body);
            log.debug("[SubscriptionCancel] 요청 {} → {}", apiUrl, jsonBody);

            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json;charset=EUC-KR");
            if (authorization != null && !authorization.isBlank()) {
                conn.setRequestProperty("Authorization", authorization);
            }
            if (token != null && !token.isBlank()) {
                conn.setRequestProperty("TOKEN", token);
            }
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(15_000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            int httpStatus = conn.getResponseCode();
            InputStream is = (httpStatus < 400) ? conn.getInputStream() : conn.getErrorStream();
            String responseBody;
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(is, Charset.forName("EUC-KR")))) {
                responseBody = br.lines().collect(Collectors.joining());
            }

            log.debug("[SubscriptionCancel] 응답 ← {}", responseBody);
            return objectMapper.readValue(responseBody, new TypeReference<>() {});

        } catch (IOException e) {
            throw new RuntimeException("키움페이 취소 API 통신 오류: " + e.getMessage(), e);
        }
    }

    private String nvl(String s) {
        return s != null ? s : "";
    }
}
