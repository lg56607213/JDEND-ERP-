package com.jdend.erp.report;

import com.jdend.erp.accounting.cash.dto.DailyFundReportResponse;
import com.jdend.erp.accounting.cash.service.DailyCashService;
import com.jdend.erp.auth.entity.LoginUser;
import com.jdend.erp.auth.repository.LoginUserRepository;
import com.jdend.erp.config.DbContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyReportScheduler {

    private final LoginUserRepository loginUserRepo;
    private final ReportEmailSettingsRepository settingsRepo;
    private final DailyCashService dailyCashService;
    private final DailyReportEmailService emailService;

    // 매일 오전 9시 (KST) 전일 자금일보 발송
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void sendDailyReports() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("[일일자금보고] 스케줄러 시작 date={}", yesterday);

        // auth DB에서 발송 설정이 활성화된 목록 조회 (DbContextHolder 미설정 = auth DB)
        List<ReportEmailSettings> enabledSettings = settingsRepo.findByEnabledTrue();
        if (enabledSettings.isEmpty()) {
            log.info("[일일자금보고] 발송 대상 없음");
            return;
        }

        // 활성 회사 목록 (companyId → LoginUser)
        List<LoginUser> companies = loginUserRepo.findAllActiveCompanies();

        for (ReportEmailSettings settings : enabledSettings) {
            String emails = settings.getRecipientEmails();
            if (emails == null || emails.isBlank()) continue;

            List<String> recipients = Arrays.stream(emails.split(","))
                    .map(String::trim)
                    .filter(e -> !e.isEmpty())
                    .toList();
            if (recipients.isEmpty()) continue;

            LoginUser company = companies.stream()
                    .filter(c -> c.getId().equals(settings.getCompanyId()))
                    .findFirst().orElse(null);
            if (company == null) continue;

            try {
                DbContextHolder.set(company.getTargetDb());
                DailyFundReportResponse report = dailyCashService.daily(yesterday);
                DbContextHolder.clear(); // auth DB로 복원 후 이메일 발송

                emailService.sendDailyReport(
                        company.getCompanyName(), yesterday, report, recipients);

            } catch (Exception e) {
                log.error("[일일자금보고] 처리 실패 company={} error={}",
                        company.getCompanyName(), e.getMessage());
            } finally {
                DbContextHolder.clear();
            }
        }

        log.info("[일일자금보고] 스케줄러 완료 처리건수={}", enabledSettings.size());
    }
}
