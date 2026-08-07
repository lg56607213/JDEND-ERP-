package com.jdend.erp.dashboard.repository;

import com.jdend.erp.accounting.voucher.entity.VoucherLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DashboardVoucherRepository extends JpaRepository<VoucherLine, Long> {

    // 일별 보통예금 전표 입금(DEBIT) / 출금(CREDIT) 합계 - 대사용
    @Query(value = """
        select v.voucher_date,
               coalesce(sum(case when vl.line_type = 'DEBIT'  then vl.amount else 0 end), 0) as debit_sum,
               coalesce(sum(case when vl.line_type = 'CREDIT' then vl.amount else 0 end), 0) as credit_sum
        from voucher_lines vl
        join vouchers v on v.id = vl.voucher_id
        where vl.account_name like '보통예금%'
          and v.voucher_date >= :from
          and v.voucher_date <= :to
        group by v.voucher_date
        order by v.voucher_date
    """, nativeQuery = true)
    List<Object[]> sumBankVoucherByDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

    // ── 계좌명별 보통예금 잔액 계산 (대시보드 보통예금 현황 전표 기준) ─────────────

    /** 특정 계좌명으로 특정일까지 누적 순잔액 (차변 합계 - 대변 합계) */
    @Query("""
        select coalesce(
            sum(case when vl.lineType = 'DEBIT' then vl.amount else -vl.amount end), 0)
        from VoucherLine vl
        join vl.voucher v
        where vl.accountName = :accountName
          and v.voucherDate <= :d
    """)
    Long sumNetUpToByAccountName(@Param("accountName") String accountName, @Param("d") LocalDate d);

    /** 특정 계좌명의 특정일 차변(수입) 합계 */
    @Query("""
        select coalesce(sum(vl.amount), 0)
        from VoucherLine vl
        join vl.voucher v
        where vl.accountName = :accountName
          and vl.lineType = 'DEBIT'
          and v.voucherDate = :d
    """)
    Long sumDebitOnByAccountName(@Param("accountName") String accountName, @Param("d") LocalDate d);

    /** 특정 계좌명의 특정일 대변(지출) 합계 */
    @Query("""
        select coalesce(sum(vl.amount), 0)
        from VoucherLine vl
        join vl.voucher v
        where vl.accountName = :accountName
          and vl.lineType = 'CREDIT'
          and v.voucherDate = :d
    """)
    Long sumCreditOnByAccountName(@Param("accountName") String accountName, @Param("d") LocalDate d);

    // ── accountCode='100101'(보통예금) 기준 집계 ──────────────────────────────

    /** 보통예금(100101) 특정일까지 누적 순잔액 (차변합 - 대변합) */
    @Query("""
        select coalesce(
            sum(case when vl.lineType = 'DEBIT' then vl.amount else -vl.amount end), 0)
        from VoucherLine vl
        join vl.voucher v
        where vl.accountCode = '100101'
          and v.voucherDate <= :d
    """)
    Long sumNetUpToByAccountCode(@Param("d") LocalDate d);

    /** 보통예금(100101) 특정일 차변(수입) 합계 */
    @Query("""
        select coalesce(sum(vl.amount), 0)
        from VoucherLine vl
        join vl.voucher v
        where vl.accountCode = '100101'
          and vl.lineType = 'DEBIT'
          and v.voucherDate = :d
    """)
    Long sumDebitOnByAccountCode(@Param("d") LocalDate d);

    /** 보통예금(100101) 특정일 대변(지출) 합계 */
    @Query("""
        select coalesce(sum(vl.amount), 0)
        from VoucherLine vl
        join vl.voucher v
        where vl.accountCode = '100101'
          and vl.lineType = 'CREDIT'
          and v.voucherDate = :d
    """)
    Long sumCreditOnByAccountCode(@Param("d") LocalDate d);
}
