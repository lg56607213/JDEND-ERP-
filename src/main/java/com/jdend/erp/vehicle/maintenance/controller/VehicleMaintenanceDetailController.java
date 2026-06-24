package com.jdend.erp.vehicle.maintenance.controller;

import com.jdend.erp.vehicle.maintenance.dto.VehicleMaintenanceItemDetailResponse;
import com.jdend.erp.vehicle.maintenance.dto.VehicleMaintenanceItemUpdateRequest;
import com.jdend.erp.vehicle.maintenance.service.VehicleMaintenanceDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/vehicle-maintenance")

public class VehicleMaintenanceDetailController {

  private final VehicleMaintenanceDetailService service;

  // ✅ 상세 조회 (정비현황 -> 상세 화면에서 호출)
  @GetMapping("/{maintenanceId}/items/{itemId}")
  public VehicleMaintenanceItemDetailResponse detail(
      @PathVariable Long maintenanceId,
      @PathVariable Long itemId
  ) {
    return service.getDetail(maintenanceId, itemId);
  }

  // ✅ 항목 수정 (상세 화면 수정 완료 버튼)
  @PutMapping("/items/{itemId}")
  public VehicleMaintenanceItemDetailResponse update(
      @PathVariable Long itemId,
      @RequestBody VehicleMaintenanceItemUpdateRequest req
  ) {
    return service.update(itemId, req);
  }
}