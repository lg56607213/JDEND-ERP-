package com.jdend.erp.payment.schedule.service;

import com.jdend.erp.contract.entity.Contract;
import com.jdend.erp.payment.schedule.entity.PaymentSchedule;
import com.jdend.erp.payment.schedule.repository.PaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentScheduleAutoGeneratorService {

  private final PaymentScheduleRepository scheduleRepo;

  /**
   * ✅ 계약 기준 스케줄 자동 생성 (멱등)
   * - 해당 contractNumber에 스케줄이 0건일 때만 생성
   */
  @Transactional // ✅ readOnly 절대 X
  public int ensureGenerated(Contract c) {
    if (c == null) return 0;
    if (c.getContractNumber() == null || c.getContractNumber().isBlank()) return 0;

    if (scheduleRepo.existsByContractNumber(c.getContractNumber())) return 0;

    int billingCount = (c.getBillingCount() == null) ? 0 : c.getBillingCount();
    if (billingCount <= 0) return 0;

    LocalDate contractStart = c.getStartDate();
    if (contractStart == null) return 0;

    Integer taxDay = c.getTaxInvoiceDay();     // nullable
    Integer billingDay = c.getBillingDay();    // nullable
    Integer payDay = parseDayFromPaymentDueDay(c.getPaymentDueDay()); // nullable

    List<PaymentSchedule> batch = new ArrayList<>();

    // 회차마다 원래 계약시작일에서 다시 계산하면(contractStart.plusMonths(i-1)) 말일 시작
    // 계약에서 윤년/월말 클램핑 때문에 회차 사이에 공백·겹침이 생긴다(예: 1/31 시작 계약의
    // 2회차가 2/28에 끝나는데 3회차는 클램핑 전 원래 날짜인 3/31부터 시작해버림).
    // 그래서 각 회차의 시작일을 직전 회차 종료일 다음날로 이어붙여 기간이 끊기지 않게 한다.
    LocalDate billStart = contractStart;

    for (int i = 1; i <= billingCount; i++) {
      LocalDate billEnd = billStart.plusMonths(1).minusDays(1);

      LocalDate taxDate = withDaySafe(billStart, (taxDay != null && taxDay > 0) ? taxDay : billStart.getDayOfMonth());

      int payDayFinal =
          (payDay != null && payDay > 0) ? payDay :
          (billingDay != null && billingDay > 0) ? billingDay :
          billStart.getDayOfMonth();

      LocalDate paymentDate = withDaySafe(billStart, payDayFinal);

      PaymentSchedule ps = PaymentSchedule.builder()
          .contract(c)
          .contractNumber(c.getContractNumber())
          .installmentNo(i)
          .billStartDate(billStart)
          .billEndDate(billEnd)
          .taxInvoiceDate(taxDate)
          .paymentDate(paymentDate)
          .rentAmount(c.getMonthlyRent())
          .principalAmount(null)
          .interestAmount(null)
          .remainingPrincipal(null)
          .build();

      batch.add(ps);

      billStart = billEnd.plusDays(1);
    }

    scheduleRepo.saveAll(batch);
    return batch.size();
  }

  private static Integer parseDayFromPaymentDueDay(String s) {
    if (s == null) return null;
    String digits = s.replaceAll("[^0-9]", "");
    if (digits.isBlank()) return null;
    try {
      int d = Integer.parseInt(digits);
      return d > 0 ? d : null;
    } catch (Exception e) {
      return null;
    }
  }

  private static LocalDate withDaySafe(LocalDate baseMonth, int day) {
    if (baseMonth == null) return null;
    if (day <= 0) day = 1;

    YearMonth ym = YearMonth.of(baseMonth.getYear(), baseMonth.getMonthValue());
    int max = ym.lengthOfMonth();
    int safeDay = Math.min(day, max);
    return LocalDate.of(baseMonth.getYear(), baseMonth.getMonthValue(), safeDay);
  }
}