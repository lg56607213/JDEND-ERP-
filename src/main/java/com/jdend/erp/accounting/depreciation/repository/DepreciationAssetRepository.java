package com.jdend.erp.accounting.depreciation.repository;

import com.jdend.erp.accounting.depreciation.entity.DepreciationAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepreciationAssetRepository extends JpaRepository<DepreciationAsset, Long> {
  Optional<DepreciationAsset> findByVehicleNo(String vehicleNo);
  Optional<DepreciationAsset> findByVehicleMgmtNo(String vehicleMgmtNo);
}