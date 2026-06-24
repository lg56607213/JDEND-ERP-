package com.jdend.erp.payment.billing.repository;

import com.jdend.erp.payment.billing.entity.BillingLines;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillingLineRepository extends JpaRepository<BillingLines, Long> {
  boolean existsByScheduleId(Long scheduleId);
  List<BillingLines> findByBillingIdOrderByInstallmentNoAsc(Long billingId);
}