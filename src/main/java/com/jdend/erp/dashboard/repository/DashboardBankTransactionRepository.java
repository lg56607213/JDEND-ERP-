package com.jdend.erp.dashboard.repository;

import com.jdend.erp.accounting.cash.entity.BankTransaction;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

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

  // 계좌별 특정일까지 누적 순잔액
  @Query("""
    select coalesce(sum(coalesce(b.depositAmount,0) - coalesce(b.withdrawalAmount,0)),0)
    from CashBankTransaction b
    where b.accountNo = :accountNo and b.txDate <= :d
  """)
  Long sumNetUpToByAccount(@Param("accountNo") String accountNo, @Param("d") LocalDate d);

  @Query("""
    select coalesce(sum(coalesce(b.depositAmount,0)),0)
    from CashBankTransaction b
    where b.accountNo = :accountNo and b.txDate = :d
  """)
  Long sumDepositOnByAccount(@Param("accountNo") String accountNo, @Param("d") LocalDate d);

  @Query("""
    select coalesce(sum(coalesce(b.withdrawalAmount,0)),0)
    from CashBankTransaction b
    where b.accountNo = :accountNo and b.txDate = :d
  """)
  Long sumWithdrawalOnByAccount(@Param("accountNo") String accountNo, @Param("d") LocalDate d);

  // 일별 전체 입금/출금 합계 (대사용)
  @Query(value = """
    select tx_date,
           coalesce(sum(deposit_amount),0) as dep,
           coalesce(sum(withdrawal_amount),0) as wit
    from bank_transactions
    where tx_date >= :from and tx_date <= :to
    group by tx_date
    order by tx_date
  """, nativeQuery = true)
  List<Object[]> sumByDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);
}