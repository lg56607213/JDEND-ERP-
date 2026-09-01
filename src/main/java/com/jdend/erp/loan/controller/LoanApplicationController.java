package com.jdend.erp.loan.controller;

import com.jdend.erp.auth.exception.ForbiddenException;
import com.jdend.erp.auth.service.AuthService;
import com.jdend.erp.loan.dto.LoanApplicationDtos.*;
import com.jdend.erp.loan.service.LoanApplicationService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 영업용 차량 증차 자금 신청.
 *
 * <p>신청은 ERP에 로그인한 업체가 직접 한다. 운영자(플랫폼 관리자)만 전체 신청을
 * 보고 상태를 바꿀 수 있다.</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/loan-applications")
public class LoanApplicationController {

    private final LoanApplicationService service;

    // ── 신청 업체 ───────────────────────────────────────────

    @GetMapping("/my-company")
    public MyCompanyResponse myCompany(HttpSession session) {
        return service.myCompany(companyId(session), companyName(session), loginId(session));
    }

    @PostMapping
    public SubmitResponse submit(@RequestBody SubmitRequest body, HttpSession session) {
        return service.submit(body, companyId(session), companyName(session), loginId(session));
    }

    @GetMapping("/mine")
    public List<MyRowResponse> mine(HttpSession session) {
        return service.myApplications(companyId(session));
    }

    @PostMapping("/{id}/cancel")
    public MyRowResponse cancel(@PathVariable Long id, HttpSession session) {
        return service.cancelMine(id, companyId(session));
    }

    // ── 운영자 ──────────────────────────────────────────────

    @GetMapping("/admin")
    public List<AdminRowResponse> adminList(
            @RequestParam(value = "status", required = false, defaultValue = "") String status,
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpSession session) {
        requireOperator(session);
        return service.adminSearch(status, from, to);
    }

    @GetMapping("/admin/summary")
    public AdminSummaryResponse adminSummary(HttpSession session) {
        requireOperator(session);
        return service.adminSummary();
    }

    @PatchMapping("/admin/{id}")
    public AdminRowResponse adminUpdate(@PathVariable Long id,
                                        @RequestBody UpdateRequest body,
                                        HttpSession session) {
        requireOperator(session);
        return service.adminUpdate(id, body);
    }

    // ── 세션 ────────────────────────────────────────────────

    /** 업체 계정만 신청할 수 있다. 운영자·세무대리인 계정에는 COMPANY_ID가 없다. */
    private Long companyId(HttpSession session) {
        Object v = session.getAttribute(AuthService.SESSION_COMPANY_ID);
        if (v == null) {
            throw new ForbiddenException("업체 계정으로 로그인해야 이용할 수 있는 메뉴입니다.");
        }
        return Long.valueOf(String.valueOf(v));
    }

    private String companyName(HttpSession session) {
        Object v = session.getAttribute(AuthService.SESSION_COMPANY_NAME);
        return v == null ? "" : String.valueOf(v);
    }

    private String loginId(HttpSession session) {
        Object v = session.getAttribute(AuthService.SESSION_LOGIN_ID);
        return v == null ? "" : String.valueOf(v);
    }

    private void requireOperator(HttpSession session) {
        Object role = session.getAttribute(AuthService.SESSION_ROLE);
        if (!"ADMIN".equals(String.valueOf(role))) {
            throw new ForbiddenException("운영자만 접근할 수 있습니다.");
        }
    }
}
