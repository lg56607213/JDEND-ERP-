package com.jdend.erp.payment.billing.repository;

import com.jdend.erp.payment.billing.entity.Contracts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ContractsRepository extends JpaRepository<Contracts, Long> {

  // ✅ 개별생성에서 customerNumber로 계약번호 목록 얻기
  @Query("""
    select c.contractNumber
    from Contracts c
    where c.customerNumber = :customerNumber
  """)
  List<String> findContractNumbersByCustomerNumber(@Param("customerNumber") String customerNumber);

  // ✅ 계약번호 돋보기 검색 (contractNumber/customerNumber 기준)
  @Query("""
    select c
    from Contracts c
    where (:kw is null or :kw = ''
      or c.contractNumber like concat('%', :kw, '%')
      or c.customerNumber like concat('%', :kw, '%')
    )
    order by c.id desc
  """)
  List<Contracts> search(@Param("kw") String kw);
}