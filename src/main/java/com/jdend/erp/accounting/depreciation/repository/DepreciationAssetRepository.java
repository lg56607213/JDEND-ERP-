package com.jdend.erp.accounting.depreciation.repository;

import com.jdend.erp.accounting.depreciation.entity.DepreciationAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepreciationAssetRepository extends JpaRepository<DepreciationAsset, Long> {
  Optional<DepreciationAsset> findByVehicleNo(String vehicleNo);
  Optional<DepreciationAsset> findByVehicleMgmtNo(String vehicleMgmtNo);

  /** BUG-13: 동일 차량번호 자산이 여러 건일 때 최신 ID 기준 1건만 반환 (NonUniqueResultException 방지) */
  Optional<DepreciationAsset> findFirstByVehicleNoOrderByIdDesc(String vehicleNo);
}