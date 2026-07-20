package com.jdend.erp.accounting.monthlyvoucher.scheduler;

import com.jdend.erp.accounting.monthlyvoucher.repository.MonthlyVoucherRuleRepository;
import com.jdend.erp.accounting.voucher.dto.VoucherCreateRequest;
import com.jdend.erp.accounting.voucher.entity.MonthlyVoucherRule;
import com.jdend.erp.accounting.voucher.service.VoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * BUG-09: processRulesForTenant를 별도 @Service 빈으로 분리해 @Transactional이 Spring AOP에 의해
 * 정상 동작하도록 한다. (같은 클래스 내 자기 호출이면 AOP 프록시가 우회되어 트랜잭션 적용 불가)
 *
 * 전표 생성 → rule 갱신이 하나의 트랜잭션으로 묶이므로, rule 갱신 실패 시 전표도 롤백되어
 * 중복 전표 생성을 방지한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonthlyVoucherRuleProcessor {

    private final MonthlyVoucherRuleRepository ruleRepo;
    private final VoucherService voucherService;

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void processOneRule(MonthlyVoucherRule rule, LocalDate today) {
        createVoucherFromRule(rule, today);
        rule.setLastRunDate(today);
        rule.setNextRunDate(calcNextRunDate(today.plusDays(1), rule.getMonthlyDay()));
        ruleRepo.save(rule);
        log.info("[월전표] 생성 완료 rule={}", rule.getId());
    }

    public void processRulesForTenant(LocalDate today) {
        List<MonthlyVoucherRule> targets =
                ruleRepo.findByActiveTrueAndNextRunDateLessThanEqual(today);

        for (MonthlyVoucherRule rule : targets) {
            try {
                processOneRule(rule, today);
            } catch (Exception e) {
                log.error("[월전표] 규칙 처리 실패 rule={} error={} — 다음 규칙으로 계속", rule.getId(), e.getMessage());
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
