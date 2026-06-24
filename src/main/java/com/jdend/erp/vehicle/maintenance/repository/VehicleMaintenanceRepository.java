package com.jdend.erp.vehicle.maintenance.repository;

import com.jdend.erp.vehicle.maintenance.entity.VehicleMaintenance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleMaintenanceRepository extends JpaRepository<VehicleMaintenance, Long> {
}