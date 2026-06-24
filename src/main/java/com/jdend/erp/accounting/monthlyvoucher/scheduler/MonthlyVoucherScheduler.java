package com.jdend.erp.accounting.monthlyvoucher.scheduler;

import com.jdend.erp.accounting.monthlyvoucher.repository.MonthlyVoucherRuleRepository;
import com.jdend.erp.accounting.voucher.dto.VoucherCreateRequest;
import com.jdend.erp.accounting.voucher.entity.MonthlyVoucherRule;
import com.jdend.erp.accounting.voucher.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MonthlyVoucherScheduler {

    private final MonthlyVoucherRuleRepository ruleRepo;
    private final VoucherService voucherService;

    @Scheduled(cron = "0 10 0 * * *")
    @Transactional
    public void runMonthlyVoucherRules() {
        LocalDate today = LocalDate.now();

        List<MonthlyVoucherRule> targets =
                ruleRepo.findByActiveTrueAndNextRunDateLessThanEqual(today);

        for (MonthlyVoucherRule rule : targets) {
            createVoucherFromRule(rule, today);

            rule.setLastRunDate(today);
            rule.setNextRunDate(calcNextRunDate(today.plusDays(1), rule.getMonthlyDay()));
            ruleRepo.save(rule);
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