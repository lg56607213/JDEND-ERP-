package com.jdend.erp.vehicle.repository;

import com.jdend.erp.vehicle.entity.VehicleOrder;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.*;

public interface VehicleOrderRepository extends JpaRepository<VehicleOrder, Long> {

    Optional<VehicleOrder> findByVehicleMgmtNo(String vehicleMgmtNo);

    Optional<VehicleOrder> findTopByOrderByVehicleMgmtNoDesc();

    boolean existsByVehicleMgmtNo(String vehicleMgmtNo);

    List<VehicleOrder> findByOrderDateBetween(LocalDate start, LocalDate end);

    List<VehicleOrder> findByOrderStatus(String status);

    List<VehicleOrder> findByOrderStatusAndOrderDateBetween(String status, LocalDate start, LocalDate end);

    Optional<VehicleOrder> findByVehicleNo(String vehicleNo);

    @Query("""
        select v
        from VehicleOrder v
        where replace(replace(trim(v.vehicleNo), ' ', ''), '-', '') =
              replace(replace(trim(:vehicleNo), ' ', ''), '-', '')
    """)
    Optional<VehicleOrder> findByVehicleNoNormalized(@Param("vehicleNo") String vehicleNo);

    @Query("""
        select v
        from VehicleOrder v
        where (:kw = '' or
          lower(coalesce(v.vehicleNo, '')) like concat('%', lower(:kw), '%') or
          lower(coalesce(v.carModel, '')) like concat('%', lower(:kw), '%') or
          lower(coalesce(v.vehicleMgmtNo, '')) like concat('%', lower(:kw), '%') or
          lower(coalesce(v.makerContractNo, '')) like concat('%', lower(:kw), '%'))
        order by v.id desc
    """)
    List<VehicleOrder> searchTop500(@Param("kw") String kw);

    // 정기검사관리 조회용
    // vehicle_orders 테이블에 검사유효기간이 들어있는 차량 조회
    @Query("""
        select v
        from VehicleOrder v
        where v.inspectionStart is not null
          and v.inspectionEnd is not null
          and (:vehicleMgmtNo is null or lower(v.vehicleMgmtNo) like concat('%', lower(:vehicleMgmtNo), '%'))
          and (:vehicleNo is null or lower(coalesce(v.vehicleNo, '')) like concat('%', lower(:vehicleNo), '%'))
          and (:validStartFrom is null or v.inspectionStart >= :validStartFrom)
          and (:validEndTo is null or v.inspectionEnd <= :validEndTo)
        order by v.inspectionEnd desc, v.inspectionStart desc, v.id desc
    """)
    List<VehicleOrder> searchInspectionVehicles(
            @Param("vehicleMgmtNo") String vehicleMgmtNo,
            @Param("vehicleNo") String vehicleNo,
            @Param("validStartFrom") LocalDate validStartFrom,
            @Param("validEndTo") LocalDate validEndTo
    );
}