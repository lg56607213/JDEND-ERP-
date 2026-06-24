package com.jdend.erp.payment.billing.repository;

import com.jdend.erp.payment.billing.entity.PaymentSchedules;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PaymentSchedulesRepository extends JpaRepository<PaymentSchedules, Long> {

  @Query("""
    select ps
    from PaymentSchedules ps
    where ps.taxInvoiceDate between :start and :end
  """)
  List<PaymentSchedules> findByTaxInvoiceDateBetween(@Param("start") LocalDate start,
                                                     @Param("end") LocalDate end);

  @Query("""
    select ps
    from PaymentSchedules ps
    where ps.taxInvoiceDate between :start and :end
      and ps.contractNumber in :contractNumbers
  """)
  List<PaymentSchedules> findByTaxInvoiceDateBetweenAndContractNumbers(@Param("start") LocalDate start,
                                                                       @Param("end") LocalDate end,
                                                                       @Param("contractNumbers") List<String> contractNumbers);
}