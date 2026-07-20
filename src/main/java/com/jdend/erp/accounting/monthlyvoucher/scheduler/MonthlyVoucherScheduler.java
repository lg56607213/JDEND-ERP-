package com.jdend.erp.accounting.monthlyvoucher.scheduler;

import com.jdend.erp.auth.repository.LoginUserRepository;
import com.jdend.erp.config.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * BUG-09: 월전표 스케줄러.
 * processRulesForTenant 로직을 별도 @Service(MonthlyVoucherRuleProcessor)로 분리하여
 * @Transactional이 Spring AOP에 의해 정상 동작하게 했다.
 * (자기 호출 문제로 같은 클래스 내 @Transactional은 동작하지 않음)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyVoucherScheduler {

    private final LoginUserRepository loginUserRepo;
    private final MonthlyVoucherRuleProcessor ruleProcessor;  // BUG-09: 별도 빈으로 분리

    @Scheduled(cron = "0 10 0 * * *")
    public void runMonthlyVoucherRules() {
        LocalDate today = LocalDate.now();
        List<String> tenantDbs = loginUserRepo.findAllActiveCompanyTargetDbs();

        for (String db : tenantDbs) {
            try {
                TenantContext.setCurrentDb(db);
                ruleProcessor.processRulesForTenant(today);
            } catch (Exception e) {
                log.error("[월전표] db={} 처리 중 오류: {}", db, e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }
}
