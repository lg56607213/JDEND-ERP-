package com.jdend.erp.vehicle.advance.repository;

import com.jdend.erp.vehicle.advance.entity.VehicleAdvance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleAdvanceRepository extends JpaRepository<VehicleAdvance, Long> {
  List<VehicleAdvance> findByVehicleOrder_IdOrderByIdAsc(Long vehicleOrderId);
  void deleteByVehicleOrder_Id(Long vehicleOrderId);
}
