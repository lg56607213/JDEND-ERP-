package com.jdend.erp.subscription.controller;

import com.jdend.erp.auth.service.AuthService;
import com.jdend.erp.config.KiwoomPayProperties;
import com.jdend.erp.subscription.dto.SubscriptionPayReadyRequest;
import com.jdend.erp.subscription.dto.SubscriptionPayReadyResponse;
import com.jdend.erp.subscription.entity.SubscriptionPayment;
import com.jdend.erp.subscription.service.SubscriptionPayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 구독 결제 컨트롤러.
 *
 * <pre>
 * POST /api/subscription/kiwoom/ready          ← jdend.co.kr (비로그인, CORS 허용)
 * POST /api/subscription/kiwoom/notify         ← 키움페이 서버 (비로그인, 서버-to-서버)
 * GET  /api/subscription/kiwoom/result         ← jdend.co.kr (비로그인, CORS 허용)
 * GET  /api/subscription/list                  ← ERP 관리자 (ADMIN 세션 필요)
 * PATCH /api/subscription/{id}/complete        ← ERP 관리자 (ADMIN 세션 필요)
 * PATCH /api/subscription/{id}/memo            ← ERP 관리자 (ADMIN 세션 필요)
 * DELETE /api/subscription/{id}/cancel         ← ERP 관리자 (ADMIN 세션 필요)
 * </pre>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/subscription")
public class SubscriptionPayController {

    private final SubscriptionPayService service;
    private final KiwoomPayProperties properties;

    // ─────────────────────────────────────────────────────────
    // 공개 엔드포인트 (비로그인, jdend.co.kr 호출)
    // ─────────────────────────────────────────────────────────

    /**
     * 결제 준비: 키움페이 해시 요청 + PENDING 저장 + 결제창 파라미터 반환.
     * CORS: jdend.co.kr 허용.
     */
    @CrossOrigin(origins = {
            "https://jdend.co.kr", "https://www.jdend.co.kr",
            "http://localhost:3000", "http://localhost:8080"
    })
    @PostMapping("/kiwoom/ready")
    public ResponseEntity<SubscriptionPayReadyResponse> ready(
            @RequestBody SubscriptionPayReadyRequest req) {
        return ResponseEntity.ok(service.ready(req));
    }

    /**
     * 키움페이 결제 완료 통지 (서버-to-서버).
     * 반드시 &lt;RESULT&gt;SUCCESS&lt;/RESULT&gt; 응답 → 재통지 방지.
     */
    @PostMapping(value = "/kiwoom/notify", produces = MediaType.TEXT_HTML_VALUE)
    public String notify(
            @RequestParam Map<String, String> params,
            HttpServletRequest request) {

        String clientIp = resolveClientIp(request);
        List<String> allowedIps = properties.getNotifyAllowIps();

        if (!CollectionUtils.isEmpty(allowedIps)) {
            if (!allowedIps.contains(clientIp)) {
                log.warn("[SubscriptionPay] 허용되지 않은 IP 통지 무시: {}", clientIp);
                return "<html><body><RESULT>SUCCESS</RESULT></body></html>";
            }
        } else {
            log.warn("[SubscriptionPay] notify-allow-ips 미설정 (개발 모드). clientIp={}", clientIp);
        }

        try {
            service.notify(params);
        } catch (Exception e) {
            log.error("[SubscriptionPay] 통지 처리 예외 (SUCCESS 응답은 정상 반환)", e);
        }

        return "<html><body><RESULT>SUCCESS</RESULT></body></html>";
    }

    /**
     * 결제 결과 조회 (결과 페이지에서 orderId로 상태 확인).
     * CORS: jdend.co.kr 허용.
     */
    @CrossOrigin(origins = {
            "https://jdend.co.kr", "https://www.jdend.co.kr",
            "http://localhost:3000", "http://localhost:8080"
    })
    @GetMapping("/kiwoom/result")
    public Map<String, Object> result(@RequestParam String orderId) {
        return service.getResult(orderId);
    }

    // ─────────────────────────────────────────────────────────
    // 관리자 전용 엔드포인트 (ADMIN 세션 필요)
    // ─────────────────────────────────────────────────────────

    /** 구독 결제 목록 조회 (status 필터 선택) */
    @GetMapping("/list")
    public ResponseEntity<?> list(
            @RequestParam(required = false) String status,
            HttpServletRequest request) {

        if (!isAdmin(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "ADMIN 권한이 필요합니다."));
        }

        List<SubscriptionPayment> list = (status != null && !status.isBlank())
                ? service.listByStatus(status)
                : service.listAll();
        return ResponseEntity.ok(list);
    }

    /** 계정 생성 완료 처리 */
    @PatchMapping("/{id}/complete")
    public ResponseEntity<?> complete(@PathVariable Long id, HttpServletRequest request) {
        if (!isAdmin(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "ADMIN 권한이 필요합니다."));
        }
        service.markAccountCreated(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    /** 메모 저장 */
    @PatchMapping("/{id}/memo")
    public ResponseEntity<?> memo(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        if (!isAdmin(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "ADMIN 권한이 필요합니다."));
        }
        service.saveMemo(id, body.getOrDefault("memo", ""));
        return ResponseEntity.ok(Map.of("success", true));
    }

    /** 구독 결제 취소 (환불) */
    @DeleteMapping("/{id}/cancel")
    public ResponseEntity<?> cancel(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest request) {

        if (!isAdmin(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "ADMIN 권한이 필요합니다."));
        }

        String reason = (body != null) ? body.getOrDefault("reason", "구독취소") : "구독취소";
        try {
            service.cancelPayment(id, reason);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("[SubscriptionPay] 취소 처리 예외", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "취소 처리 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────
    // 내부 유틸
    // ─────────────────────────────────────────────────────────

    private boolean isAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return false;
        String role = (String) session.getAttribute(AuthService.SESSION_ROLE);
        return "ADMIN".equals(role);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
