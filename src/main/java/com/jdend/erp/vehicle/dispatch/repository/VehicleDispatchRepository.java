package com.jdend.erp.vehicle.dispatch.repository;

import com.jdend.erp.vehicle.dispatch.entity.VehicleDispatch;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface VehicleDispatchRepository extends JpaRepository<VehicleDispatch, Long> {

  @Query("""
    select d
    from VehicleDispatch d
    where (:contractNumber is null or d.contractNumber like concat('%', :contractNumber, '%'))
      and (:vehicleNo is null or d.vehicleNo like concat('%', :vehicleNo, '%'))
      and (:startDate is null or d.dispatchDate >= :startDate)
      and (:endDate is null or d.dispatchDate <= :endDate)
    order by d.dispatchDate desc, d.id desc
  """)
  List<VehicleDispatch> search(
    @Param("contractNumber") String contractNumber,
    @Param("vehicleNo") String vehicleNo,
    @Param("startDate") LocalDate startDate,
    @Param("endDate") LocalDate endDate
  );
}