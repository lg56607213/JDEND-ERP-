package com.jdend.erp.vehicle.repository;

import com.jdend.erp.vehicle.entity.VehicleLoanScheduleLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleLoanScheduleLineRepository extends JpaRepository<VehicleLoanScheduleLine, Long> {

    List<VehicleLoanScheduleLine> findByLoanIdAndScheduleTypeOrderByInstallmentNoAsc(Long loanId, String scheduleType);

    void deleteByLoanIdAndScheduleType(Long loanId, String scheduleType);
}