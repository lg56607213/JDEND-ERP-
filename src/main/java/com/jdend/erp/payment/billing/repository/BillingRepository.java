package com.jdend.erp.payment.billing.repository;

import com.jdend.erp.payment.billing.entity.Billings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BillingRepository extends JpaRepository<Billings, Long> {

  Optional<Billings> findByBillingNo(String billingNo);

  @Query("""
    select b
    from Billings b
    where (:startDate is null or b.billingDate >= :startDate)
      and (:endDate is null or b.billingDate <= :endDate)
      and (:customerName is null or :customerName = '' or lower(b.customerName) like lower(concat('%', :customerName, '%')))
      and (:contractNumber is null or :contractNumber = '' or b.contractNumber like concat('%', :contractNumber, '%'))
    order by b.id desc
  """)
  List<Billings> search(
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate,
      @Param("customerName") String customerName,
      @Param("contractNumber") String contractNumber
  );
}