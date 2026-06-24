package com.jdend.erp.accounting.cash.repository;

import com.jdend.erp.accounting.voucher.entity.VoucherLine;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface VoucherCashAggRepository extends JpaRepository<VoucherLine, Long> {

  interface DayCashSumRow {
    LocalDate getVoucherDate();
    String getLineType(); // DEBIT / CREDIT
    Long getAmt();
  }

  interface AccountCashSumRow {
    String getAccountName();
    String getLineType(); // DEBIT / CREDIT
    Long getAmt();
  }

  // ✅ "현금/예금/은행/국민/하나/신한" 키워드 포함 계정만 "현금성계정"으로 보고 집계
  @Query("""
    select v.voucherDate as voucherDate,
           l.lineType as lineType,
           coalesce(sum(l.amount),0) as amt
      from VoucherLine l
      join l.voucher v
     where v.status = '승인'
       and v.voucherDate between :start and :end
       and (
            l.accountName like %:k1% or l.accountName like %:k2% or l.accountName like %:k3%
         or l.accountName like %:k4% or l.accountName like %:k5% or l.accountName like %:k6%
         or l.accountName like %:k7%
       )
     group by v.voucherDate, l.lineType
     order by v.voucherDate
  """)
  List<DayCashSumRow> sumCashByDay(
      @Param("start") LocalDate start,
      @Param("end") LocalDate end,
      @Param("k1") String k1,
      @Param("k2") String k2,
      @Param("k3") String k3,
      @Param("k4") String k4,
      @Param("k5") String k5,
      @Param("k6") String k6,
      @Param("k7") String k7
  );

  @Query("""
    select l.accountName as accountName,
           l.lineType as lineType,
           coalesce(sum(l.amount),0) as amt
      from VoucherLine l
      join l.voucher v
     where v.status = '승인'
       and v.voucherDate = :date
       and (
            l.accountName like %:k1% or l.accountName like %:k2% or l.accountName like %:k3%
         or l.accountName like %:k4% or l.accountName like %:k5% or l.accountName like %:k6%
         or l.accountName like %:k7%
       )
     group by l.accountName, l.lineType
     order by l.lineType, l.accountName
  """)
  List<AccountCashSumRow> sumCashByAccountOn(
      @Param("date") LocalDate date,
      @Param("k1") String k1,
      @Param("k2") String k2,
      @Param("k3") String k3,
      @Param("k4") String k4,
      @Param("k5") String k5,
      @Param("k6") String k6,
      @Param("k7") String k7
  );
}