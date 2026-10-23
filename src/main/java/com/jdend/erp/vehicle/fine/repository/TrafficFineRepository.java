package com.jdend.erp.vehicle.fine.repository;

import com.jdend.erp.vehicle.fine.entity.TrafficFine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TrafficFineRepository extends JpaRepository<TrafficFine, Long> {

  @Query("""
    select f
    from TrafficFine f
    where (:vehicleNo is null or f.vehicleNo like concat('%', :vehicleNo, '%'))
      and (:customerName is null or f.customerName like concat('%', :customerName, '%'))
      and (:status is null or f.status = :status)
      and (:from is null or f.violationAt >= :from)
      and (:to is null or f.violationAt < :to)
    order by f.violationAt desc, f.id desc
  """)
  List<TrafficFine> search(
    @Param("vehicleNo") String vehicleNo,
    @Param("customerName") String customerName,
    @Param("status") String status,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to
  );
}
