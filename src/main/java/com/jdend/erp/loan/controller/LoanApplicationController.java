package com.jdend.erp.loan.controller;

import com.jdend.erp.auth.service.AuthService;
import com.jdend.erp.loan.dto.LoanApplicationDtos.*;
import com.jdend.erp.loan.service.LoanApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 대출(할부·리스) 신청.
 *
 * <p>/api/loan-apply/** 는 고객이 로그인 없이 쓰는 공개 경로다.
 * 회사코드로 대상 업체만 특정할 뿐, 다른 정보는 노출하지 않는다.</p>
 */
@RestController
@RequiredArgsConstructor
public class LoanApplicationController {

    private final LoanApplicationService service;

    // ── 고객용 (공개) ───────────────────────────────────────

    @GetMapping("/api/loan-apply/company")
    public PublicCompanyResponse company(@RequestParam("c") String companyCode) {
        return service.publicCompany(companyCode);
    }

    @PostMapping("/api/loan-apply/submit")
    public SubmitResponse submit(@RequestBody SubmitRequest body, HttpServletRequest http) {
        return service.submit(body, clientIp(http));
    }

    // ── 직원용 (로그인 필요) ────────────────────────────────

    @GetMapping("/api/loan-applications")
    public List<RowResponse> list(
            @RequestParam(value = "status", required = false, defaultValue = "") String status,
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpSession session) {
        return service.search(companyCode(session), status, from, to);
    }

    @GetMapping("/api/loan-applications/summary")
    public SummaryResponse summary(HttpServletRequest http, HttpSession session) {
        return service.summary(companyCode(session), baseUrl(http));
    }

    @PatchMapping("/api/loan-applications/{id}")
    public RowResponse update(@PathVariable Long id,
                              @RequestBody UpdateRequest body,
                              HttpSession session) {
        return service.update(id, companyCode(session), body);
    }

    // ── 내부 ────────────────────────────────────────────────

    private String companyCode(HttpSession session) {
        Object v = session.getAttribute(AuthService.SESSION_LOGIN_ID);
        if (v == null) throw new IllegalArgumentException("로그인이 필요합니다.");
        return String.valueOf(v);
    }

    private String baseUrl(HttpServletRequest req) {
        String proto = header(req, "X-Forwarded-Proto");
        String host = header(req, "X-Forwarded-Host");
        if (host != null) return (proto != null ? proto : "https") + "://" + host;

        String scheme = req.getScheme();
        int port = req.getServerPort();
        boolean defaultPort = ("http".equals(scheme) && port == 80)
                || ("https".equals(scheme) && port == 443);
        return scheme + "://" + req.getServerName() + (defaultPort ? "" : ":" + port);
    }

    private String clientIp(HttpServletRequest req) {
        String forwarded = header(req, "X-Forwarded-For");
        if (forwarded != null) {
            int comma = forwarded.indexOf(',');
            return comma > 0 ? forwarded.substring(0, comma).trim() : forwarded;
        }
        return req.getRemoteAddr();
    }

    private String header(HttpServletRequest req, String name) {
        String v = req.getHeader(name);
        return (v == null || v.isBlank()) ? null : v.trim();
    }
}
