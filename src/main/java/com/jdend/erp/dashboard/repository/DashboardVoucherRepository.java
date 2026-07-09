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
}
