package com.jdend.erp.payment.banktx.repository;

import com.jdend.erp.payment.banktx.entity.BankTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PaymentBankTransactionRepository extends JpaRepository<BankTransaction, Long> {

  boolean existsByRowHash(String rowHash);

  @Query("""
    select t
    from BankTransaction t
    where (:bank = '' or t.bankName like concat('%', :bank, '%'))
      and (:accountNo = '' or t.accountNo like concat('%', :accountNo, '%'))
      and (:startDate is null or t.txDate >= :startDate)
      and (:endDate is null or t.txDate <= :endDate)
    order by t.txDate desc, t.id desc
  """)
  List<BankTransaction> search(
      @Param("bank") String bank,
      @Param("accountNo") String accountNo,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate
  );

  // ✅ 돋보기용: (은행명/계좌번호) distinct 목록
  @Query("""
    select distinct t.bankName, t.accountNo
    from BankTransaction t
    where (:kw = '' or t.bankName like concat('%', :kw, '%') or t.accountNo like concat('%', :kw, '%'))
    order by t.bankName asc, t.accountNo asc
  """)
  List<Object[]> distinctAccounts(@Param("kw") String kw);
}