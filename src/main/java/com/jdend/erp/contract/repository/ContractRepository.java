package com.jdend.erp.contract.repository;

import com.jdend.erp.contract.dto.ContractStatusRowResponse;
import com.jdend.erp.contract.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ContractRepository extends JpaRepository<Contract, Long> {

  boolean existsByContractNumber(String contractNumber);

  @Query("select max(c.contractNumber) from Contract c")
  String findMaxContractNumber();

  @Query("SELECT MAX(c.contractNumber) FROM Contract c WHERE c.contractNumber LIKE CONCAT(:prefix, '%')")
  Optional<String> findMaxContractNumberByPrefix(@Param("prefix") String prefix);

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
      null,
      cust.customerName,
      c.startDate,
      c.endDate,
      c.monthlyRent,
      c.totalRent,
      0L,
      c.advancePayment,
      c.deposit,
      c.vehicleModel
    )
    from Contract c
    left join c.customer cust
    where 1=1
      and (:contractNumber = '' or lower(c.contractNumber) like concat('%', lower(:contractNumber), '%'))
      and (:customerName = '' or lower(cust.customerName) like concat('%', lower(:customerName), '%'))
      and (:vehicleNo = '' or lower(c.vehicleNo) like concat('%', lower(:vehicleNo), '%'))
    order by c.id desc
  """)
  List<ContractStatusRowResponse> statusList(
      @Param("contractNumber") String contractNumber,
      @Param("customerName") String customerName,
      @Param("vehicleNo") String vehicleNo
  );

  // ✅ BillingService에서 사용
  @Query("""
    select c.contractNumber
    from Contract c
    where c.customerNumber = :customerNumber
  """)
  List<String> findContractNumbersByCustomerNumber(@Param("customerNumber") String customerNumber);

  // ✅ 청구생성 계약구분(장기/단기) 필터용
  @Query("""
    select c.contractNumber
    from Contract c
    where c.contractCategory = :category
  """)
  List<String> findContractNumbersByContractCategory(@Param("category") String category);

  // ✅ 연체현황: 종료/해지 상태가 아닌 모든 계약
  @Query("""
    select c
    from Contract c
    left join fetch c.customer cust
    where c.status not in (
      '종료','만기종료','해지','중도해지','중도상환','만기상환','완료','종결'
    )
    order by c.id asc
  """)
  List<Contract> findAllActiveWithCustomer();

  // ✅ 계약만료 스케줄러: 종료일이 지났고 아직 종료 처리되지 않은 계약
  @Query("""
    select c from Contract c
    where c.endDate < :today
      and c.status not in :terminated
  """)
  List<Contract> findExpiredNotClosed(
      @Param("today") LocalDate today,
      @Param("terminated") Collection<String> terminated
  );

  // ✅ 보증금/선수금 관리: deposit > 0 OR advancePayment > 0 인 계약 조회 (고객 fetch join)
  @Query("""
    select c
    from Contract c
    left join fetch c.customer cust
    where (c.deposit > 0 or c.advancePayment > 0)
      and (:customerName = '' or lower(cust.customerName) like concat('%', lower(:customerName), '%'))
      and (:startDate is null or c.endDate >= :startDate)
      and (:endDate is null or c.startDate <= :endDate)
    order by c.id desc
  """)
  List<Contract> findDepositContracts(
      @Param("customerName") String customerName,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate
  );

  // ✅ 선수금 관리: 지정 ID 목록 중 필터 조건에 맞는 계약 조회 (고객 fetch join)
  @Query("""
    select c
    from Contract c
    left join fetch c.customer cust
    where c.id in :ids
      and (:customerName = '' or lower(cust.customerName) like concat('%', lower(:customerName), '%'))
      and (:startDate is null or c.endDate >= :startDate)
      and (:endDate is null or c.startDate <= :endDate)
    order by c.id desc
  """)
  List<Contract> findByIdsWithCustomerAndFilter(
      @Param("ids") List<Long> ids,
      @Param("customerName") String customerName,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate
  );
}