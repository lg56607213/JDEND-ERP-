package com.jdend.erp.payment.codef.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdend.erp.config.CodefProperties;
import com.jdend.erp.payment.banktx.entity.BankTransaction;
import com.jdend.erp.payment.banktx.repository.PaymentBankTransactionRepository;
import com.jdend.erp.payment.codef.entity.CodefConnection;
import com.jdend.erp.payment.codef.repository.CodefConnectionRepository;
import com.jdend.erp.payment.codef.util.CodefCryptoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodefService {

    private final CodefProperties properties;
    private final CodefTokenService tokenService;
    private final CodefCryptoUtil cryptoUtil;
    private final CodefConnectionRepository connectionRepository;
    private final PaymentBankTransactionRepository bankTxRepository;
    private final ObjectMapper objectMapper;

    // ─────────────────────────────────────────────────────────
    // 공동인증서 등록 (Connected ID 생성)
    // ─────────────────────────────────────────────────────────

    @Transactional
    public CodefConnection registerCertificate(
            byte[] certFile,
            byte[] keyFile,
            String certPassword,
            String organization,
            String bankName,
            String accountNo,
            String clientType) {

        String encryptedPassword = cryptoUtil.encryptPassword(certPassword);
        // CODEF는 개인키 파일(SignPri.key)을 certFile로, 공인인증서(SignCert.der)를 derCert로 받음
        String encodedCert = cryptoUtil.encodeFile(keyFile);
        String encodedDer  = cryptoUtil.encodeFile(certFile);

        Map<String, Object> account = new LinkedHashMap<>();
        account.put("countryCode", "KR");
        account.put("businessType", "BK");
        account.put("clientType", clientType != null ? clientType : "B");
        account.put("organization", organization);
        account.put("loginType", "1");
        account.put("certType", "0");
        account.put("certFile", encodedCert);   // SignPri.key (개인키)
        account.put("derCert",  encodedDer);    // SignCert.der (공인인증서)
        account.put("certPassword", encryptedPassword);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("accountList", List.of(account));

        Map<String, Object> response = callCodefApi("/v1/account/create", body);

        String resultCode = getResultCode(response);
        if (!"CF-00000".equals(resultCode)) {
            String msg = getResultMessage(response);
            throw new RuntimeException("CODEF 인증서 등록 실패: " + resultCode + " - " + msg);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        String connectedId = (String) data.get("connectedId");

        CodefConnection conn = CodefConnection.builder()
                .connectedId(connectedId)
                .organization(organization)
                .bankName(bankName)
                .accountNo(accountNo)
                .clientType(clientType != null ? clientType : "B")
                .active(true)
                .build();

        connectionRepository.save(conn);
        log.info("[CODEF] 인증서 등록 완료: bank={}, connectedId={}", bankName, connectedId);
        return conn;
    }

    // ─────────────────────────────────────────────────────────
    // 은행 거래내역 수동 최신화
    // ─────────────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> syncTransactions(Long connectionId, String startDate, String endDate) {
        CodefConnection conn = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new IllegalArgumentException("등록된 계좌 없음: id=" + connectionId));

        String apiPath = "B".equals(conn.getClientType())
                ? "/v1/kr/bank/b/account/transaction-list"
                : "/v1/kr/bank/p/account/transaction-list";

        String start = startDate != null ? startDate : LocalDate.now().minusDays(30).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String end   = endDate   != null ? endDate   : LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("connectedId", conn.getConnectedId());
        body.put("organization", conn.getOrganization());
        if (conn.getAccountNo() != null && !conn.getAccountNo().isBlank()) {
            body.put("account", conn.getAccountNo());
        }
        body.put("startDate", start);
        body.put("endDate", end);
        body.put("orderBy", "0");

        Map<String, Object> response = callCodefApi(apiPath, body);

        String resultCode = getResultCode(response);
        if (!"CF-00000".equals(resultCode)) {
            throw new RuntimeException("CODEF 거래내역 조회 실패: " + resultCode + " - " + getResultMessage(response));
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.get("data");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> txList = (List<Map<String, Object>>) data.getOrDefault("resTrHistoryList", List.of());

        int saved = 0;
        int skipped = 0;
        String batchId = UUID.randomUUID().toString().substring(0, 8) + "-codef";

        for (Map<String, Object> tx : txList) {
            String txDate   = str(tx, "resAccountTrDate");
            String txTime   = str(tx, "resAccountTrTime");
            String inAmt    = str(tx, "resAccountIn");
            String outAmt   = str(tx, "resAccountOut");
            String balance  = str(tx, "resAfterTrBalance");
            String remark1  = str(tx, "resAccountTrRemark1");
            String remark2  = str(tx, "resAccountTrRemark2");

            long deposit    = parseLong(inAmt);
            long withdrawal = parseLong(outAmt);
            long bal        = parseLong(balance);

            String summary = remark1.isBlank() ? remark2 : remark1;
            String rowHash = sha256(conn.getBankName() + "|" + conn.getAccountNo() + "|"
                    + txDate + txTime + "|" + deposit + "|" + withdrawal + "|" + summary);

            if (bankTxRepository.existsByRowHash(rowHash)) {
                skipped++;
                continue;
            }

            LocalDate date = parseDate(txDate);
            BankTransaction entity = BankTransaction.builder()
                    .bankName(conn.getBankName())
                    .accountNo(conn.getAccountNo() != null ? conn.getAccountNo() : "")
                    .txDate(date)
                    .depositAmount(deposit)
                    .withdrawalAmount(withdrawal)
                    .balance(bal)
                    .summary(summary)
                    .importBatchId(batchId)
                    .rowHash(rowHash)
                    .build();

            bankTxRepository.save(entity);
            saved++;
        }

        log.info("[CODEF] 거래내역 동기화: bank={}, 저장={}, 중복skip={}", conn.getBankName(), saved, skipped);
        return Map.of("saved", saved, "skipped", skipped, "total", txList.size());
    }

    // ─────────────────────────────────────────────────────────
    // 내부 HTTP 호출
    // ─────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, Object> callCodefApi(String path, Map<String, Object> body) {
        try {
            String token = tokenService.getAccessToken();
            String jsonBody = objectMapper.writeValueAsString(body);

            URL url = new URL(properties.getBaseUrl() + path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(30_000);
            conn.setReadTimeout(60_000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            InputStream is = (status < 400) ? conn.getInputStream() : conn.getErrorStream();
            String response;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                response = br.lines().collect(Collectors.joining());
            }

            // CODEF API 응답은 URL 인코딩되어 있음 (%7B%22...형태) → 디코딩 후 파싱
            if (response.startsWith("%")) {
                response = java.net.URLDecoder.decode(response, StandardCharsets.UTF_8);
            }
            log.info("[CODEF] {} 전체응답: {}", path, response);
            return objectMapper.readValue(response, new TypeReference<>() {});

        } catch (IOException e) {
            throw new RuntimeException("CODEF API 호출 실패 [" + path + "]: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────
    // 유틸
    // ─────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private String getResultCode(Map<String, Object> response) {
        Map<String, Object> result = (Map<String, Object>) response.get("result");
        return result != null ? String.valueOf(result.getOrDefault("code", "")) : "";
    }

    @SuppressWarnings("unchecked")
    private String getResultMessage(Map<String, Object> response) {
        Map<String, Object> result = (Map<String, Object>) response.get("result");
        return result != null ? String.valueOf(result.getOrDefault("message", "")) : "";
    }

    private String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString().trim() : "";
    }

    private long parseLong(String s) {
        if (s == null || s.isBlank()) return 0L;
        try { return Long.parseLong(s.replaceAll("[^0-9]", "")); }
        catch (NumberFormatException e) { return 0L; }
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.length() < 8) return LocalDate.now();
        try { return LocalDate.parse(s.substring(0, 8), DateTimeFormatter.ofPattern("yyyyMMdd")); }
        catch (Exception e) { return LocalDate.now(); }
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }
}
