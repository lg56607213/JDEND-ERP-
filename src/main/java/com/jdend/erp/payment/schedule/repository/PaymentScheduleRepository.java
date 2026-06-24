package com.jdend.erp.payment.schedule.repository;

import com.jdend.erp.payment.schedule.entity.PaymentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PaymentScheduleRepository extends JpaRepository<PaymentSchedule, Long> {

  List<PaymentSchedule> findByContractNumberOrderByInstallmentNoAsc(String contractNumber);

  Optional<PaymentSchedule> findByContractNumberAndInstallmentNo(String contractNumber, Integer installmentNo);

  boolean existsByContractNumber(String contractNumber);

  long countByContractNumber(String contractNumber);

  // ✅ 청구생성 핵심: 세금계산서일자 기간
  List<PaymentSchedule> findByTaxInvoiceDateBetween(LocalDate taxStartDate, LocalDate taxEndDate);
}