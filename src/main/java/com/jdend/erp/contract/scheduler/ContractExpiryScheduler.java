package com.jdend.erp.contract.scheduler;

import com.jdend.erp.auth.repository.LoginUserRepository;
import com.jdend.erp.config.TenantContext;
import com.jdend.erp.contract.service.ContractExpiryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContractExpiryScheduler {

    private final LoginUserRepository loginUserRepo;
    private final ContractExpiryService expiryService;

    // 앱 시작 시 즉시 실행 (기존 만료 계약 일괄 처리)
    @EventListener(ApplicationReadyEvent.class)
    public void runOnStartup() {
        run();
    }

    // 매일 00:05 실행
    @Scheduled(cron = "0 5 0 * * *")
    public void run() {
        LocalDate today = LocalDate.now();

        // auth DB에서 활성 회사 목록 조회 (DbContextHolder 미설정 상태 = auth DB)
        List<String> tenantDbs = loginUserRepo.findAllActiveCompanyTargetDbs();

        for (String db : tenantDbs) {
            try {
                TenantContext.setCurrentDb(db);
                int count = expiryService.expireForTenant(today);
                if (count > 0) {
                    log.info("[계약만료] db={} 처리건수={}", db, count);
                }
            } catch (Exception e) {
                log.error("[계약만료] db={} 처리 중 오류: {}", db, e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }
}
