package com.jdend.erp.vehicle.repository;

import com.jdend.erp.vehicle.entity.VehicleOrderHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleOrderHistoryRepository extends JpaRepository<VehicleOrderHistory, Long> {
  List<VehicleOrderHistory> findByVehicleOrder_IdOrderByChangedAtAsc(Long vehicleOrderId);
}
