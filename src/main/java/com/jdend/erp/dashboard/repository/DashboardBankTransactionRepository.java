package com.jdend.erp.dashboard.repository;

import com.jdend.erp.accounting.cash.entity.BankTransaction;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface DashboardBankTransactionRepository extends JpaRepository<BankTransaction, Long> {

  @Query("""
    select coalesce(sum(coalesce(b.depositAmount,0) - coalesce(b.withdrawalAmount,0)),0)
    from CashBankTransaction b
    where b.txDate < :d
  """)
  Long sumNetBefore(@Param("d") LocalDate d);

  @Query("""
    select coalesce(sum(coalesce(b.depositAmount,0)),0)
    from CashBankTransaction b
    where b.txDate = :d
  """)
  Long sumDepositOn(@Param("d") LocalDate d);

  @Query("""
    select coalesce(sum(coalesce(b.withdrawalAmount,0)),0)
    from CashBankTransaction b
    where b.txDate = :d
  """)
  Long sumWithdrawalOn(@Param("d") LocalDate d);
}