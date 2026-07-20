package com.jdend.erp.report;

import com.jdend.erp.accounting.cash.dto.DailyFundReportResponse;
import com.jdend.erp.accounting.cash.service.DailyCashService;
import com.jdend.erp.auth.entity.LoginUser;
import com.jdend.erp.auth.repository.LoginUserRepository;
import com.jdend.erp.auth.service.AuthService;
import com.jdend.erp.config.TenantContext;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/report-email-settings")
public class ReportEmailSettingsController {

    private final ReportEmailSettingsRepository settingsRepo;
    private final DailyCashService dailyCashService;
    private final DailyReportEmailService emailService;
    private final LoginUserRepository loginUserRepo;

    /** 현재 회사의 설정 조회 */
    @GetMapping
    public Map<String, Object> get(HttpSession session) {
        Long companyId = (Long) session.getAttribute(AuthService.SESSION_COMPANY_ID);
        if (companyId == null) {
            return Map.of("enabled", false, "recipients", List.of());
        }

        String savedDb = TenantContext.getCurrentDb();
        try {
            TenantContext.setCurrentDb("auth");
            ReportEmailSettings s = settingsRepo.findByCompanyId(companyId).orElse(null);
            if (s == null) return Map.of("enabled", false, "recipients", List.of());
            List<String> recipients = parseEmails(s.getRecipientEmails());
            return Map.of("enabled", Boolean.TRUE.equals(s.getEnabled()), "recipients", recipients);
        } finally {
            if (savedDb != null) TenantContext.setCurrentDb(savedDb);
            else TenantContext.clear();
        }
    }

    /** 설정 저장 */
    @PostMapping
    public Map<String, Object> save(@RequestBody Map<String, Object> body, HttpSession session) {
        Long companyId = (Long) session.getAttribute(AuthService.SESSION_COMPANY_ID);
        if (companyId == null) {
            return Map.of("success", false, "message", "로그인 세션이 만료되었습니다.");
        }

        boolean enabled = Boolean.TRUE.equals(body.get("enabled"));

        @SuppressWarnings("unchecked")
        List<String> recipientList = body.get("recipients") instanceof List<?>
                ? (List<String>) body.get("recipients") : List.of();

        String recipientEmails = String.join(",", recipientList.stream()
                .map(String::trim).filter(e -> !e.isEmpty()).toList());

        String savedDb = TenantContext.getCurrentDb();
        try {
            TenantContext.setCurrentDb("auth");
            ReportEmailSettings s = settingsRepo.findByCompanyId(companyId)
                    .orElse(ReportEmailSettings.builder().companyId(companyId).build());
            s.setEnabled(enabled);
            s.setRecipientEmails(recipientEmails);
            settingsRepo.save(s);
            return Map.of("success", true, "message", "저장되었습니다.");
        } catch (Exception e) {
            return Map.of("success", false, "message", "저장 중 오류: " + e.getMessage());
        } finally {
            if (savedDb != null) TenantContext.setCurrentDb(savedDb);
            else TenantContext.clear();
        }
    }

    /** 테스트 발송 — 오늘 기준 어제 데이터로 즉시 발송 */
    @PostMapping("/test")
    public Map<String, Object> sendTest(@RequestBody Map<String, Object> body, HttpSession session) {
        Long companyId = (Long) session.getAttribute(AuthService.SESSION_COMPANY_ID);
        if (companyId == null) {
            return Map.of("success", false, "message", "로그인 세션이 만료되었습니다.");
        }

        @SuppressWarnings("unchecked")
        List<String> recipients = body.get("recipients") instanceof List<?>
                ? (List<String>) body.get("recipients") : List.of();
        if (recipients.isEmpty()) {
            return Map.of("success", false, "message", "수신자 이메일을 입력해주세요.");
        }

        try {
            // 회사 정보 조회 (auth DB)
            TenantContext.setCurrentDb("auth");
            LoginUser company = loginUserRepo.findById(companyId).orElse(null);
            if (company == null) {
                return Map.of("success", false, "message", "회사 정보를 찾을 수 없습니다.");
            }

            // 어제 자금일보 조회 (회사 DB)
            TenantContext.setCurrentDb(company.getTargetDb());
            DailyFundReportResponse report = dailyCashService.daily(LocalDate.now().minusDays(1));

            // 이메일 발송
            TenantContext.clear();
            emailService.sendDailyReport(
                    company.getCompanyName(),
                    LocalDate.now().minusDays(1),
                    report,
                    recipients);

            return Map.of("success", true, "message", "테스트 메일을 발송했습니다. 받은편지함을 확인해주세요.");
        } catch (Exception e) {
            return Map.of("success", false, "message", "발송 실패: " + e.getMessage());
        } finally {
            TenantContext.clear();
        }
    }

    private List<String> parseEmails(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split(","))
                .map(String::trim).filter(e -> !e.isEmpty()).toList();
    }
}
