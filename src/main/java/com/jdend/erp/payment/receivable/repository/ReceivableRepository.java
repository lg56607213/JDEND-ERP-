package com.jdend.erp.payment.receivable.repository;

import com.jdend.erp.payment.receivable.entity.Receivable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ReceivableRepository extends JpaRepository<Receivable, Long> {

  @Query("""
    select r
    from Receivable r
    where
      (:startDate is null or r.receivableDate >= :startDate)
      and (:endDate is null or r.receivableDate <= :endDate)
      and (:customerName = '' or lower(r.customerName) like concat('%', lower(:customerName), '%'))
      and (:status = '' or r.status = :status)
    order by r.id desc
  """)
  List<Receivable> search(
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate,
      @Param("customerName") String customerName,
      @Param("status") String status
  );
}