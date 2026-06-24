package com.jdend.erp.accounting.statements.repository;

import com.jdend.erp.accounting.statements.dto.BalanceDetailRowResponse;
import com.jdend.erp.accounting.voucher.entity.VoucherLine;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface StatementAggRepository extends JpaRepository<VoucherLine, Long> {

  interface LineSumRow {
    String getAccountName();
    String getLineType(); // DEBIT / CREDIT
    Long getAmt();
  }

  @Query("""
    select l.accountName as accountName,
           l.lineType as lineType,
           sum(l.amount) as amt
      from VoucherLine l
      join l.voucher v
     where v.voucherDate between :start and :end
       and (:status is null or :status = '' or v.status = :status)
     group by l.accountName, l.lineType
  """)
  List<LineSumRow> sumByAccountBetween(
      @Param("start") LocalDate start,
      @Param("end") LocalDate end,
      @Param("status") String status
  );

  @Query("""
    select l.accountName as accountName,
           l.lineType as lineType,
           sum(l.amount) as amt
      from VoucherLine l
      join l.voucher v
     where v.voucherDate <= :ref
       and (:status is null or :status = '' or v.status = :status)
     group by l.accountName, l.lineType
  """)
  List<LineSumRow> sumByAccountToDate(
      @Param("ref") LocalDate ref,
      @Param("status") String status
  );

  @Query("""
    select new com.jdend.erp.accounting.statements.dto.BalanceDetailRowResponse(
      v.voucherDate,
      v.voucherNo,
      l.lineType,
      l.accountName,
      l.amount,
      l.description,
      v.contractNumber,
      v.vehicleNo,
      v.memo,
      v.status
    )
    from VoucherLine l
    join l.voucher v
    where (:startDate is null or v.voucherDate >= :startDate)
      and v.voucherDate <= :referenceDate
      and l.accountName in :accountNames
      and (:status is null or :status = '' or v.status = :status)
    order by v.voucherDate desc, v.id desc, l.sortOrder asc
  """)
  List<BalanceDetailRowResponse> findBalanceDetails(
      @Param("startDate") LocalDate startDate,
      @Param("referenceDate") LocalDate referenceDate,
      @Param("accountNames") List<String> accountNames,
      @Param("status") String status
  );
}