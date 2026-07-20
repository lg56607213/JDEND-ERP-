package com.jdend.erp.accounting.monthlyvoucher.scheduler;

import com.jdend.erp.accounting.monthlyvoucher.repository.MonthlyVoucherRuleRepository;
import com.jdend.erp.accounting.voucher.dto.VoucherCreateRequest;
import com.jdend.erp.accounting.voucher.entity.MonthlyVoucherRule;
import com.jdend.erp.accounting.voucher.service.VoucherService;
import com.jdend.erp.auth.repository.LoginUserRepository;
import com.jdend.erp.config.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyVoucherScheduler {

    private final LoginUserRepository loginUserRepo;
    private final MonthlyVoucherRuleRepository ruleRepo;
    private final VoucherService voucherService;

    @Scheduled(cron = "0 10 0 * * *")
    public void runMonthlyVoucherRules() {
        LocalDate today = LocalDate.now();
        List<String> tenantDbs = loginUserRepo.findAllActiveCompanyTargetDbs();

        for (String db : tenantDbs) {
            try {
                TenantContext.setCurrentDb(db);
                processRulesForTenant(today);
            } catch (Exception e) {
                log.error("[월전표] db={} 처리 중 오류: {}", db, e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }

    private void processRulesForTenant(LocalDate today) {
        List<MonthlyVoucherRule> targets =
                ruleRepo.findByActiveTrueAndNextRunDateLessThanEqual(today);

        for (MonthlyVoucherRule rule : targets) {
            try {
                createVoucherFromRule(rule, today);
                rule.setLastRunDate(today);
                rule.setNextRunDate(calcNextRunDate(today.plusDays(1), rule.getMonthlyDay()));
                ruleRepo.save(rule);
                log.info("[월전표] 생성 완료 db={} rule={}", TenantContext.getCurrentDb(), rule.getId());
            } catch (Exception e) {
                log.error("[월전표] 규칙 처리 실패 rule={} error={}", rule.getId(), e.getMessage());
            }
        }
    }

    private void createVoucherFromRule(MonthlyVoucherRule rule, LocalDate voucherDate) {
        String memo = "[월전표]";
        if (rule.getMemo() != null && !rule.getMemo().isBlank()) {
            memo += " " + rule.getMemo().trim();
        }

        voucherService.create(
                VoucherCreateRequest.builder()
                        .voucherDate(voucherDate)
                        .contractNumber(blankToNull(rule.getContractNumber()))
                        .vehicleNo(blankToNull(rule.getVehicleNo()))
                        .memo(memo)
                        .debitEntries(List.of(
                                VoucherCreateRequest.VoucherLineRequest.builder()
                                        .account(rule.getDebitAccount())
                                        .amount(rule.getDebitAmount())
                                        .description(blankToNull(rule.getDebitDescription()))
                                        .build()
                        ))
                        .creditEntries(List.of(
                                VoucherCreateRequest.VoucherLineRequest.builder()
                                        .account(rule.getCreditAccount())
                                        .amount(rule.getCreditAmount())
                                        .description(blankToNull(rule.getCreditDescription()))
                                        .build()
                        ))
                        .build()
        );
    }

    private LocalDate calcNextRunDate(LocalDate base, int day) {
        LocalDate thisMonth = clampDay(base.withDayOfMonth(1), day);
        if (!thisMonth.isBefore(base)) return thisMonth;

        LocalDate nextMonthFirst = base.plusMonths(1).withDayOfMonth(1);
        return clampDay(nextMonthFirst, day);
    }

    private LocalDate clampDay(LocalDate monthFirst, int day) {
        int last = monthFirst.lengthOfMonth();
        return monthFirst.withDayOfMonth(Math.min(day, last));
    }

    private String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
