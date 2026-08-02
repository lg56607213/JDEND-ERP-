package com.jdend.erp.accounting.depreciation.scheduler;

import com.jdend.erp.accounting.depreciation.dto.PostDepreciationRequest;
import com.jdend.erp.accounting.depreciation.repository.DepreciationAssetRepository;
import com.jdend.erp.accounting.depreciation.service.DepreciationService;
import com.jdend.erp.auth.repository.LoginUserRepository;
import com.jdend.erp.config.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DepreciationScheduler {

    private final LoginUserRepository loginUserRepo;
    private final DepreciationAssetRepository assetRepo;
    private final DepreciationService depreciationService;

    /** 매달 말일 01:00 — 전월 감가상각 전표 자동 생성 */
    @Scheduled(cron = "0 0 1 L * *")
    public void runMonthlyDepreciation() {
        LocalDate today = LocalDate.now();
        String baseMonth = today.format(DateTimeFormatter.ofPattern("yyyy-MM"));

        List<String> tenantDbs = loginUserRepo.findAllActiveCompanyTargetDbs();
        log.info("[감가상각 자동] 실행 시작 — baseMonth={}, 테넌트 수={}", baseMonth, tenantDbs.size());

        for (String db : tenantDbs) {
            try {
                TenantContext.setCurrentDb(db);

                List<Long> assetIds = assetRepo.findAll().stream()
                        .filter(a -> Boolean.TRUE.equals(a.getActive()))
                        .map(a -> a.getId())
                        .toList();

                if (assetIds.isEmpty()) {
                    log.info("[감가상각 자동] db={} 활성 자산 없음, 스킵", db);
                    continue;
                }

                PostDepreciationRequest req = PostDepreciationRequest.builder()
                        .baseMonth(baseMonth)
                        .voucherDate(today)
                        .assetIds(assetIds)
                        .build();

                Map<String, Object> result = depreciationService.postDepreciation(req);
                log.info("[감가상각 자동] db={} 완료 — 전표생성={}, 스킵={}",
                        db, result.get("voucherCreatedCount"), result.get("skippedCount"));

            } catch (Exception e) {
                log.error("[감가상각 자동] db={} 오류: {}", db, e.getMessage(), e);
            } finally {
                TenantContext.clear();
            }
        }
    }
}
