package com.jdend.erp.accounting.cash.repository;

import com.jdend.erp.accounting.cash.entity.BankTransaction;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {

  interface DaySumRow {
    LocalDate getTxDate();
    Long getInAmt();
    Long getOutAmt();
  }

  interface BankSumRow {
    String getBankName();
    Long getInAmt();
    Long getOutAmt();
  }

  @Query("""
    select b.txDate as txDate,
           coalesce(sum(coalesce(b.depositAmount,0)),0) as inAmt,
           coalesce(sum(coalesce(b.withdrawalAmount,0)),0) as outAmt
      from CashBankTransaction b
     where b.txDate between :start and :end
     group by b.txDate
     order by b.txDate
  """)
  List<DaySumRow> sumByDay(@Param("start") LocalDate start, @Param("end") LocalDate end);

  @Query("""
    select coalesce(b.bankName,'(미지정)') as bankName,
           coalesce(sum(coalesce(b.depositAmount,0)),0) as inAmt,
           coalesce(sum(coalesce(b.withdrawalAmount,0)),0) as outAmt
      from CashBankTransaction b
     where b.txDate = :date
     group by b.bankName
     order by b.bankName
  """)
  List<BankSumRow> sumByBankOn(@Param("date") LocalDate date);
}