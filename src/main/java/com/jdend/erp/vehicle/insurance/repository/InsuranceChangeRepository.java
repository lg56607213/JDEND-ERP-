package com.jdend.erp.vehicle.insurance.repository;

import com.jdend.erp.vehicle.insurance.entity.InsuranceChange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InsuranceChangeRepository extends JpaRepository<InsuranceChange, Long> {
    List<InsuranceChange> findByInsuranceIdOrderByIdDesc(Long insuranceId);
}
