package com.jdend.erp.config;

import com.jdend.erp.auth.repository.LoginUserRepository;
import com.jdend.erp.management.financial.entity.FinancialStatementAccount;
import com.jdend.erp.management.financial.repository.FinancialStatementAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 앱 시작 시 모든 테넌트 DB에 필수 재무제표 계정이 없으면 자동으로 추가한다.
 * TenantSchemaUpdateRunner(@Order(1)/ApplicationRunner) 완료 후
 * ApplicationReadyEvent 순서로 실행되므로 스키마는 이미 최신 상태임.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FinancialAccountSeedRunner {

    private final LoginUserRepository loginUserRepo;
    private final FinancialStatementAccountRepository fsAccountRepo;

    @EventListener(ApplicationReadyEvent.class)
    public void seedOnStartup() {
        List<String> tenantDbs = loginUserRepo.findAllActiveCompanyTargetDbs();

        if (tenantDbs.isEmpty()) {
            log.info("[FinancialSeed] 활성 테넌트 없음, 스킵");
            return;
        }

        log.info("[FinancialSeed] {}개 테넌트 계정 시드 시작", tenantDbs.size());

        for (String db : tenantDbs) {
            try {
                TenantContext.setCurrentDb(db);
                seedRentalDeposit(db);
            } catch (Exception e) {
                log.error("[FinancialSeed] db={} 처리 오류: {}", db, e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }

        log.info("[FinancialSeed] 완료");
    }

    /**
     * 임대보증금 계정(200202)이 없으면 비유동부채(2002) 하위에 추가한다.
     */
    private void seedRentalDeposit(String db) {
        if (fsAccountRepo.existsByAccountCode("200202")) {
            log.debug("[FinancialSeed] db={} 200202(임대보증금) 이미 존재, 스킵", db);
            return;
        }

        // 부모 계정 '2002'(비유동부채) 조회
        Optional<FinancialStatementAccount> parentOpt = fsAccountRepo.findByAccountCode("2002");
        Long parentId = parentOpt.map(FinancialStatementAccount::getId).orElse(null);

        if (parentId == null) {
            log.warn("[FinancialSeed] db={} 부모계정 2002(비유동부채) 없음 — parentId=null 로 추가", db);
        }

        FinancialStatementAccount account = FinancialStatementAccount.builder()
                .statementType("bs")
                .category("LIABILITY")
                .level(3)
                .parentId(parentId)
                .accountCode("200202")
                .accountName("임대보증금")
                .accountType("비유동부채")
                .displayOrder(2)
                .isActive("사용")
                .isPostable("사용")
                .build();

        fsAccountRepo.save(account);
        log.info("[FinancialSeed] db={} 200202(임대보증금) 추가 완료 (parentId={})", db, parentId);
    }
}
