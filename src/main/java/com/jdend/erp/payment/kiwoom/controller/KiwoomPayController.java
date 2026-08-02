package com.jdend.erp.payment.kiwoom.controller;

import com.jdend.erp.config.KiwoomPayProperties;
import com.jdend.erp.payment.kiwoom.dto.KiwoomPayReadyRequest;
import com.jdend.erp.payment.kiwoom.dto.KiwoomPayReadyResponse;
import com.jdend.erp.payment.kiwoom.service.KiwoomPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 키움페이 LINK V3.1 결제 컨트롤러.
 *
 * <pre>
 * POST /api/payments/kiwoom/ready   ← ERP 프론트 → 백엔드 (결제 준비)
 * POST /api/payments/kiwoom/notify  ← 키움페이 서버 → 우리 서버 (결제 완료 통지)
 * GET  /api/payments/kiwoom/result  ← ERP 프론트 → 백엔드 (결제 결과 조회)
 * </pre>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments/kiwoom")
public class KiwoomPayController {

    private final KiwoomPayService service;
    private final KiwoomPayProperties properties;

    // -------------------------------------------------------------------------
    // 1. 결제 준비 — 프론트가 호출
    // -------------------------------------------------------------------------

    /**
     * 해시(KIWOOM_ENC) 발급 및 결제창 파라미터 반환.
     * 응답의 payUrl 로 form POST 하면 키움페이 결제창이 열린다.
     */
    @PostMapping("/ready")
    public KiwoomPayReadyResponse ready(@RequestBody KiwoomPayReadyRequest req) {
        return service.preparePayment(req);
    }

    // -------------------------------------------------------------------------
    // 2. 결제 완료 통지 — 키움페이 서버가 호출
    // -------------------------------------------------------------------------

    /**
     * 키움페이 서버 → POST 통지.
     * 반드시 {@code <RESULT>SUCCESS</RESULT>} 포함 HTML 로 응답해야 재통지가 멈춤.
     *
     * <p>보안: 키움페이 공인 IP 목록(kiwoompay.notify-allow-ips)만 허용.
     * 목록이 비어 있으면 개발 모드로 간주하여 IP 검증 생략(경고 로그).
     */
    @PostMapping(value = "/notify", produces = MediaType.TEXT_HTML_VALUE)
    public String notify(
            @RequestParam Map<String, String> params,
            HttpServletRequest request) {

        String clientIp = resolveClientIp(request);

        // IP 검증 (허용 목록이 비어 있으면 개발 모드 — 경고만 출력)
        List<String> allowedIps = properties.getNotifyAllowIps();
        if (!CollectionUtils.isEmpty(allowedIps)) {
            if (!allowedIps.contains(clientIp)) {
                log.warn("[KiwoomPay] 허용되지 않은 IP 에서 통지 수신 — 무시: {}", clientIp);
                // 보안상 거부하되 키움페이에는 SUCCESS 반환(불필요한 재통지 방지)
                return "<html><body><RESULT>SUCCESS</RESULT></body></html>";
            }
        } else {
            log.warn("[KiwoomPay] notify-allow-ips 미설정 — IP 검증 생략 (개발 모드). 운영 배포 전 반드시 설정할 것. clientIp={}", clientIp);
        }

        try {
            service.processNotify(params);
        } catch (Exception e) {
            // 예외가 발생해도 반드시 SUCCESS 응답 — 아니면 키움페이가 1시간 재통지
            log.error("[KiwoomPay] 통지 처리 중 예외 발생", e);
        }

        return "<html><body><RESULT>SUCCESS</RESULT></body></html>";
    }

    // -------------------------------------------------------------------------
    // 3. 결제 결과 조회 — 결제 완료 페이지(kiwoom_result.html)가 호출
    // -------------------------------------------------------------------------

    /**
     * orderId 로 KiwoomPayment 를 조회하여 status 등을 반환.
     * 통지보다 먼저 도달할 수 있으므로 PENDING 상태도 그대로 반환.
     */
    @GetMapping("/result")
    public Map<String, Object> result(@RequestParam String orderId) {
        return service.getResult(orderId);
    }

    // -------------------------------------------------------------------------
    // 내부 유틸
    // -------------------------------------------------------------------------

    /** 프록시(로드밸런서) 뒤에서도 실제 클라이언트 IP 를 추출 */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
