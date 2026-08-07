package com.jdend.erp.payment.codef.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdend.erp.config.CodefProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodefTokenService {

    private final CodefProperties properties;
    private final ObjectMapper objectMapper;

    private String cachedToken;
    private Instant tokenExpiry;

    public synchronized String getAccessToken() {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiry)) {
            return cachedToken;
        }
        cachedToken = requestNewToken();
        tokenExpiry = Instant.now().plusSeconds(3000); // 50분 캐시 (만료 1시간)
        log.info("[CODEF] 새 액세스 토큰 발급 완료");
        return cachedToken;
    }

    @SuppressWarnings("unchecked")
    private String requestNewToken() {
        try {
            String credentials = properties.getClientId() + ":" + properties.getClientSecret();
            String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

            URL url = new URL(properties.getOauthUrl() + "/oauth/token");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Authorization", "Basic " + encoded);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(15_000);

            String body = "grant_type=client_credentials&scope=read";
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            InputStream is = (status < 400) ? conn.getInputStream() : conn.getErrorStream();
            String response;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                response = br.lines().collect(Collectors.joining());
            }

            Map<String, Object> resp = objectMapper.readValue(response, Map.class);
            String token = (String) resp.get("access_token");
            if (token == null) {
                throw new RuntimeException("CODEF 토큰 응답에 access_token 없음: " + response);
            }
            return token;

        } catch (IOException e) {
            throw new RuntimeException("CODEF OAuth 토큰 요청 실패: " + e.getMessage(), e);
        }
    }
}
