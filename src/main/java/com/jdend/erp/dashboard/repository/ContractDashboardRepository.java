package com.jdend.erp.dashboard.repository;

import com.jdend.erp.contract.entity.Contract;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ContractDashboardRepository extends JpaRepository<Contract, Long> {

  // 차량번호 기준으로 장기 계약 차량
  @Query("""
    select distinct c.vehicleNo
    from Contract c
    where trim(c.contractType) = '장기'
  """)
  List<String> findLongTermVehicleNos();

  // 차량번호 기준으로 단기 계약 차량
  @Query("""
    select distinct c.vehicleNo
    from Contract c
    where trim(c.contractType) = '단기'
  """)
  List<String> findShortTermVehicleNos();

  // 차량번호 기준 계약 존재 여부
  @Query("""
    select distinct c.vehicleNo
    from Contract c
  """)
  List<String> findAllContractVehicleNos();
}