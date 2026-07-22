package com.jdend.erp.dashboard.repository;

import com.jdend.erp.contract.entity.Contract;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ContractDashboardRepository extends JpaRepository<Contract, Long> {

  // 차량번호 기준으로 장기 계약 차량 (BUG-7차-05: 해지 제외, BUG-8차-05: 월렌트 등 구형 타입 포함,
  //   BUG-10차-05: 영문 타입 'long' 추가)
  @Query("""
    select distinct c.vehicleNo
    from Contract c
    where trim(c.contractType) in ('장기', '월렌트', '장기렌트', '월간렌트', 'long')
      and (c.status is null or trim(c.status) <> '해지')
  """)
  List<String> findLongTermVehicleNos();

  // 차량번호 기준으로 단기 계약 차량 (BUG-7차-05: 해지 계약 제외, BUG-10차-05: 영문 타입 'short' 추가)
  @Query("""
    select distinct c.vehicleNo
    from Contract c
    where trim(c.contractType) in ('단기', 'short')
      and (c.status is null or trim(c.status) <> '해지')
  """)
  List<String> findShortTermVehicleNos();

  // 차량번호 기준 계약 존재 여부
  @Query("""
    select distinct c.vehicleNo
    from Contract c
  """)
  List<String> findAllContractVehicleNos();
}