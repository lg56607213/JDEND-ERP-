package com.jdend.erp.vehicle.maintenance.repository;

import com.jdend.erp.vehicle.maintenance.dto.VehicleMaintenanceStatusRowResponse;
import com.jdend.erp.vehicle.maintenance.entity.VehicleMaintenanceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface VehicleMaintenanceItemRepository extends JpaRepository<VehicleMaintenanceItem, Long> {

  Optional<VehicleMaintenanceItem> findByIdAndMaintenance_Id(Long itemId, Long maintenanceId);

  @Query("""
    select new com.jdend.erp.vehicle.maintenance.dto.VehicleMaintenanceStatusRowResponse(
      m.id,
      i.id,
      m.vehicleMgmtNo,
      m.vehicleNo,
      i.payDate,
      i.description,
      i.amount,
      i.supplyAmount,
      i.vatAmount,
      i.vendor,
      i.paymentMethod
    )
    from VehicleMaintenanceItem i
    join i.maintenance m
    where (:mgmt = '' or m.vehicleMgmtNo like concat('%', :mgmt, '%'))
      and (:vno = '' or m.vehicleNo like concat('%', :vno, '%'))
      and (:startDate is null or i.payDate >= :startDate)
      and (:endDate is null or i.payDate <= :endDate)
    order by i.id desc
  """)
  List<VehicleMaintenanceStatusRowResponse> searchStatus(
      @Param("mgmt") String mgmt,
      @Param("vno") String vno,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate
  );
}