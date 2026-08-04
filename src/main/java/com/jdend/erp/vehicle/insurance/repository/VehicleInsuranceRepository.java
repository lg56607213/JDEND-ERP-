package com.jdend.erp.vehicle.insurance.repository;

import com.jdend.erp.vehicle.insurance.entity.VehicleInsurance;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface VehicleInsuranceRepository extends JpaRepository<VehicleInsurance, Long> {

  @Query("""
    select i
    from VehicleInsurance i
    where (:contractNumber is null or i.contractNumber like concat('%', :contractNumber, '%'))
      and (:vehicleNo is null or i.vehicleNo like concat('%', :vehicleNo, '%'))
      and (:startDate is null or i.insuranceStartDate >= :startDate)
      and (:endDate is null or i.insuranceStartDate <= :endDate)
    order by i.insuranceStartDate desc, i.id desc
  """)
  List<VehicleInsurance> search(
    @Param("contractNumber") String contractNumber,
    @Param("vehicleNo") String vehicleNo,
    @Param("startDate") LocalDate startDate,
    @Param("endDate") LocalDate endDate
  );

  @Query("SELECT MAX(i.insuranceEndDate) FROM VehicleInsurance i WHERE i.vehicleMgmtNo = :mgmtNo")
  Optional<LocalDate> findLatestEndDateByVehicleMgmtNo(@Param("mgmtNo") String mgmtNo);
}