package com.jdend.erp.accounting.monthlyvoucher.service;

import com.jdend.erp.accounting.monthlyvoucher.dto.MonthlyVoucherRuleCreateRequest;
import com.jdend.erp.accounting.monthlyvoucher.dto.MonthlyVoucherRuleCreateResponse;
import com.jdend.erp.accounting.monthlyvoucher.entity.VehicleOrderMini;
import com.jdend.erp.accounting.monthlyvoucher.repository.MonthlyVoucherRuleRepository;
import com.jdend.erp.accounting.monthlyvoucher.repository.VehicleOrderMiniRepository;

// ✅ 기존 엔티티 사용
import com.jdend.erp.accounting.voucher.entity.MonthlyVoucherRule;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class MonthlyVoucherRuleService {

  private final MonthlyVoucherRuleRepository ruleRepo;
  private final VehicleOrderMiniRepository vehicleOrderRepo;

  @Transactional
  public MonthlyVoucherRuleCreateResponse create(MonthlyVoucherRuleCreateRequest req) {

    if (req.getMonthlyDate() == null || req.getMonthlyDate() < 1 || req.getMonthlyDate() > 31) {
      throw new IllegalArgumentException("monthlyDate는 1~31 사이여야 합니다.");
    }
    if (req.getDebitAccount() == null || req.getDebitAccount().isBlank()) {
      throw new IllegalArgumentException("차변 계정명은 필수입니다.");
    }
    if (req.getCreditAccount() == null || req.getCreditAccount().isBlank()) {
      throw new IllegalArgumentException("대변 계정명은 필수입니다.");
    }

    long debit = req.getDebitAmount() == null ? 0 : req.getDebitAmount();
    long credit = req.getCreditAmount() == null ? 0 : req.getCreditAmount();

    if (debit <= 0 || credit <= 0) {
      throw new IllegalArgumentException("금액은 0보다 커야 합니다.");
    }
    if (debit != credit) {
      throw new IllegalArgumentException("차변/대변 금액이 일치하지 않습니다.");
    }

    // 차량관리번호 -> 차량번호(vehicle_no)
    String vehicleNo = null;
    if (req.getVehicleManagementId() != null && !req.getVehicleManagementId().isBlank()) {
      VehicleOrderMini vo = vehicleOrderRepo.findTop1ByVehicleMgmtNo(req.getVehicleManagementId().trim())
          .orElseThrow(() -> new IllegalArgumentException(
              "해당 차량관리번호를 vehicle_orders에서 찾을 수 없습니다: " + req.getVehicleManagementId()
          ));
      vehicleNo = vo.getVehicleNo();
    }

    LocalDate today = LocalDate.now();
    LocalDate nextRun = calcNextRunDate(today, req.getMonthlyDate());

    MonthlyVoucherRule rule = MonthlyVoucherRule.builder()
        .active(true) // ✅ 기존 엔티티 필드
        .contractNumber(blankToNull(req.getContractNumber()))
        .vehicleNo(blankToNull(vehicleNo))
        .monthlyDay(req.getMonthlyDate())
        .nextRunDate(nextRun)
        .lastRunDate(null)
        .debitAccount(req.getDebitAccount().trim())
        .debitAmount(debit)
        .debitDescription(blankToNull(req.getDebitDescription()))
        .creditAccount(req.getCreditAccount().trim())
        .creditAmount(credit)
        .creditDescription(blankToNull(req.getCreditDescription()))
        .memo(blankToNull(req.getMemo()))
        .build();

    MonthlyVoucherRule saved = ruleRepo.save(rule);

    // ✅ boolean getter는 Lombok이 isActive()로 만들어줌
    return MonthlyVoucherRuleCreateResponse.builder()
        .id(saved.getId())
        .isActive(saved.isActive())
        .contractNumber(saved.getContractNumber())
        .vehicleNo(saved.getVehicleNo())
        .monthlyDay(saved.getMonthlyDay())
        .nextRunDate(saved.getNextRunDate())
        .debitAccount(saved.getDebitAccount())
        .debitAmount(saved.getDebitAmount())
        .creditAccount(saved.getCreditAccount())
        .creditAmount(saved.getCreditAmount())
        .build();
  }

  private LocalDate calcNextRunDate(LocalDate base, int day) {
    LocalDate thisMonth = clampDay(base.withDayOfMonth(1), day);
    if (!thisMonth.isBefore(base)) return thisMonth; // 오늘 포함

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