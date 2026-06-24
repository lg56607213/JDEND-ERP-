package com.jdend.erp.vehicle.inspection.repository;

import com.jdend.erp.vehicle.inspection.entity.VehicleInspection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface VehicleInspectionRepository
    extends JpaRepository<VehicleInspection, Long>, JpaSpecificationExecutor<VehicleInspection> {
}