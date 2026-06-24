package com.jdend.erp.dashboard.repository;

import com.jdend.erp.dashboard.dto.DashboardInsuranceRow;
import com.jdend.erp.vehicle.insurance.entity.VehicleInsurance;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface VehicleInsuranceDashboardRepository extends JpaRepository<VehicleInsurance, Long> {

  // ✅ 계약 테이블을 아예 안 봄
  // vehicle_insurances 테이블에 보험 등록된 건만 조회
  @Query("""
    select i
    from VehicleInsurance i
    where i.insuranceEndDate is not null
      and i.insuranceEndDate between :from and :to
    order by i.insuranceEndDate asc, i.id desc
  """)
  List<VehicleInsurance> findInsuranceEntitiesExpiring(
      @Param("from") LocalDate from,
      @Param("to") LocalDate to
  );

  default List<DashboardInsuranceRow> findInsuranceExpiring(LocalDate from, LocalDate to, int limit) {
    int safeLimit = limit <= 0 ? 5 : limit;

    return findInsuranceEntitiesExpiring(from, to).stream()
        .limit(safeLimit)
        .map(i -> DashboardInsuranceRow.builder()
            .contractNumber(i.getContractNumber() == null ? "" : i.getContractNumber())
            .customerName("")
            .vehicleNo(i.getVehicleNo() == null ? "" : i.getVehicleNo())
            .insuranceEndDate(i.getInsuranceEndDate())
            .dday(0)
            .build()
        )
        .toList();
  }
}