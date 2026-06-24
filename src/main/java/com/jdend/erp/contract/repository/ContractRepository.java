package com.jdend.erp.contract.repository;

import com.jdend.erp.contract.dto.ContractStatusRowResponse;
import com.jdend.erp.contract.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ContractRepository extends JpaRepository<Contract, Long> {

  boolean existsByContractNumber(String contractNumber);

  @Query("select max(c.contractNumber) from Contract c")
  String findMaxContractNumber();

  Optional<Contract> findByContractNumber(String contractNumber);

  @Query("""
    select c
    from Contract c
    left join fetch c.customer cust
    where c.contractNumber = :contractNumber
  """)
  Optional<Contract> findWithCustomerByContractNumber(@Param("contractNumber") String contractNumber);

  @Query("""
    select (count(c) > 0)
    from Contract c
    where replace(replace(trim(c.vehicleNo), ' ', ''), '-', '') =
          replace(replace(trim(:vehicleNo), ' ', ''), '-', '')
  """)
  boolean existsByVehicleNoNormalized(@Param("vehicleNo") String vehicleNo);

  @Query("""
    select c
    from Contract c
    left join fetch c.customer cust
    where (:kw = '' or
           c.contractNumber like concat('%', :kw, '%') or
           c.vehicleNo like concat('%', :kw, '%') or
           cust.customerName like concat('%', :kw, '%'))
    order by c.id desc
  """)
  List<Contract> searchTop200(@Param("kw") String kw);

  @Query(value = """
      SELECT c.contract_number
      FROM contracts c
      WHERE REPLACE(REPLACE(TRIM(IFNULL(c.vehicle_no,'')), ' ', ''), '-', '')
          = REPLACE(REPLACE(TRIM(IFNULL(:vehicleNo,'')), ' ', ''), '-', '')
      ORDER BY c.id DESC
      LIMIT 1
      """, nativeQuery = true)
  Optional<String> findLatestContractNumberByVehicleNo(@Param("vehicleNo") String vehicleNo);

  @Query("""
    select c
    from Contract c
    left join fetch c.customer cust
    where
      (c.endDate is null or c.endDate >= current_date)
      and (
        c.status is null or trim(c.status) = '' or
        c.status not in (
          '종료','만기종료','해지','중도해지','중도상환','만기상환','완료','종결'
        )
      )
      and (
        :kw = '' or
        c.contractNumber like concat('%', :kw, '%') or
        c.vehicleNo like concat('%', :kw, '%') or
        cust.customerName like concat('%', :kw, '%')
      )
    order by c.id desc
  """)
  List<Contract> payableSearchTop200(@Param("kw") String kw);

  @Query("""
    select new com.jdend.erp.contract.dto.ContractStatusRowResponse(
      c.vehicleNo,
      c.contractNumber,
      c.status,
      cust.customerName,
      c.startDate,
      c.endDate,
      c.monthlyRent,
      c.totalRent,
      0L,
      c.advancePayment,
      c.vehicleModel
    )
    from Contract c
    left join c.customer cust
    where 1=1
      and (:contractNumber = '' or lower(c.contractNumber) like concat('%', lower(:contractNumber), '%'))
      and (:customerName = '' or lower(cust.customerName) like concat('%', lower(:customerName), '%'))
      and (:vehicleNo = '' or lower(c.vehicleNo) like concat('%', lower(:vehicleNo), '%'))
      and (:status = '' or c.status = :status)
    order by c.id desc
  """)
  List<ContractStatusRowResponse> statusList(
      @Param("contractNumber") String contractNumber,
      @Param("customerName") String customerName,
      @Param("vehicleNo") String vehicleNo,
      @Param("status") String status
  );

  // ✅ BillingService에서 사용
  @Query("""
    select c.contractNumber
    from Contract c
    where c.customerNumber = :customerNumber
  """)
  List<String> findContractNumbersByCustomerNumber(@Param("customerNumber") String customerNumber);
}