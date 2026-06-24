package com.jdend.erp.accounting.monthlyvoucher.repository;

import com.jdend.erp.accounting.monthlyvoucher.entity.VehicleOrderMini;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VehicleOrderMiniRepository extends JpaRepository<VehicleOrderMini, Long> {
  Optional<VehicleOrderMini> findTop1ByVehicleMgmtNo(String vehicleMgmtNo);
}