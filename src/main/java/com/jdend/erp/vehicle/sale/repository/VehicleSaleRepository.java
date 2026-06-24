package com.jdend.erp.vehicle.sale.repository;

import com.jdend.erp.vehicle.sale.entity.VehicleSale;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface VehicleSaleRepository extends JpaRepository<VehicleSale, Long> {

  @Query("""
    select s
    from VehicleSale s
    where (:startDate is null or s.saleDate >= :startDate)
      and (:endDate is null or s.saleDate <= :endDate)
      and (:buyer is null or s.buyer like concat('%', :buyer, '%'))
    order by s.saleDate desc, s.id desc
  """)
  List<VehicleSale> search(
    @Param("startDate") LocalDate startDate,
    @Param("endDate") LocalDate endDate,
    @Param("buyer") String buyer
  );
}