package com.jdend.erp.accounting.monthlyvoucher.repository;

import com.jdend.erp.accounting.voucher.entity.MonthlyVoucherRule; // ✅ 기존 엔티티 사용
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MonthlyVoucherRuleRepository extends JpaRepository<MonthlyVoucherRule, Long> {
  List<MonthlyVoucherRule> findByActiveTrueAndNextRunDateLessThanEqual(LocalDate date);
}