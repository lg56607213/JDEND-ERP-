package com.jdend.erp.accounting.voucher.repository;

import com.jdend.erp.accounting.voucher.entity.Voucher;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    boolean existsByVoucherNo(String voucherNo);

    @Query("select count(v) from Voucher v where v.voucherDate = :date")
    long countByVoucherDate(@Param("date") LocalDate date);

    List<Voucher> findByMemoStartingWithOrderByIdAsc(String memoPrefix);

    @Query("""
        select v
        from Voucher v
        where (:date is null or v.voucherDate = :date)
          and (:status is null or :status = '' or v.status = :status)
        order by v.voucherDate desc, v.id desc
    """)
    List<Voucher> searchForApproval(@Param("date") LocalDate date, @Param("status") String status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Voucher v
           set v.status = '승인'
         where v.id in :ids
    """)
    int approveByIds(@Param("ids") List<Long> ids);
}