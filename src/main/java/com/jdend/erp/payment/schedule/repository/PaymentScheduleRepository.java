package com.jdend.erp.payment.schedule.repository;

import com.jdend.erp.payment.schedule.entity.PaymentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

  // BUG-05: 계약 수정 시 미래 미수납 스케줄만 삭제 (billStartDate >= 기준일인 건)
  @Modifying
  @Query("delete from PaymentSchedule ps where ps.contractNumber = :contractNumber and ps.billStartDate >= :fromDate")
  int deleteByContractNumberAndBillStartDateGreaterThanEqual(@Param("contractNumber") String contractNumber, @Param("fromDate") LocalDate fromDate);

  // BUG-07: 연체 조회 N+1 개선 - 계약번호 목록으로 일괄 조회
  @Query("select ps from PaymentSchedule ps where ps.contractNumber in :contractNumbers order by ps.contractNumber asc, ps.installmentNo asc")
  List<PaymentSchedule> findByContractNumberIn(@Param("contractNumbers") List<String> contractNumbers);
}