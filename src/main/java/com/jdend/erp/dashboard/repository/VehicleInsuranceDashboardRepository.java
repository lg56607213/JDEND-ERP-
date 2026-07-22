package com.jdend.erp.dashboard.repository;

import com.jdend.erp.contract.entity.Contract;
import com.jdend.erp.dashboard.dto.DashboardInsuranceRow;
import com.jdend.erp.vehicle.insurance.entity.VehicleInsurance;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface VehicleInsuranceDashboardRepository extends JpaRepository<VehicleInsurance, Long> {

  // BUG-9차-03: 해지 계약에 연결된 보험은 만기 알림에서 제외
  @Query("""
    select i
    from VehicleInsurance i
    where i.insuranceEndDate is not null
      and i.insuranceEndDate between :from and :to
      and (i.contractNumber is null
        or i.contractNumber not in (
          select c.contractNumber from Contract c where trim(c.status) = '해지'
        )
      )
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