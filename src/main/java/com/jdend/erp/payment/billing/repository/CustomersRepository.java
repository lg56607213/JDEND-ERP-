package com.jdend.erp.payment.billing.repository;

import com.jdend.erp.payment.billing.entity.Customers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CustomersRepository extends JpaRepository<Customers, Long> {

  Customers findFirstByCustomerNumber(String customerNumber);

  @Query("""
    select c
    from Customers c
    where (:kw is null or :kw = ''
          or lower(c.customerName) like lower(concat('%', :kw, '%'))
          or lower(c.customerNumber) like lower(concat('%', :kw, '%')))
    order by c.id desc
  """)
  List<Customers> search(@Param("kw") String kw);
}