package com.jdend.erp.vehicle.repository;

import com.jdend.erp.vehicle.entity.VehicleLoan;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VehicleLoanRepository extends JpaRepository<VehicleLoan, Long> {

    @Query("""
        select l
        from VehicleLoan l
        left join fetch l.vehicleOrder o
        where l.id = :id
    """)
    Optional<VehicleLoan> findDetail(@Param("id") Long id);

    @Query("""
        select l
        from VehicleLoan l
        left join fetch l.vehicleOrder o
        where 1=1
          and (:vehicleNo = '' or lower(coalesce(o.vehicleNo,'')) like concat('%', lower(:vehicleNo), '%'))
          and (:financeName = '' or lower(coalesce(l.financeName,'')) like concat('%', lower(:financeName), '%'))
          and (:terminated is null or l.terminated = :terminated)
        order by l.id desc
    """)
    List<VehicleLoan> search(
            @Param("vehicleNo") String vehicleNo,
            @Param("financeName") String financeName,
            @Param("terminated") Boolean terminated
    );

    Optional<VehicleLoan> findTopByVehicleOrder_IdOrderByIdDesc(Long vehicleOrderId);

    @Query("""
        select l
        from VehicleLoan l
        join fetch l.vehicleOrder o
        where replace(replace(trim(o.vehicleNo), ' ', ''), '-', '') =
              replace(replace(trim(:vehicleNo), ' ', ''), '-', '')
        order by l.id desc
    """)
    List<VehicleLoan> findByVehicleNoNormalizedOrderByIdDesc(@Param("vehicleNo") String vehicleNo);

    @Query("""
        select l
        from VehicleLoan l
        join fetch l.vehicleOrder o
        where (l.terminated = false or l.terminated is null)
          and (
                :kw = '' or
                lower(coalesce(o.vehicleNo, '')) like concat('%', lower(:kw), '%') or
                lower(coalesce(o.vehicleMgmtNo, '')) like concat('%', lower(:kw), '%') or
                lower(coalesce(o.carModel, '')) like concat('%', lower(:kw), '%') or
                lower(coalesce(o.makerContractNo, '')) like concat('%', lower(:kw), '%') or
                lower(coalesce(l.financeName, '')) like concat('%', lower(:kw), '%')
              )
        order by l.id desc
    """)
    List<VehicleLoan> searchLoanVehiclePicker(@Param("kw") String kw);
}