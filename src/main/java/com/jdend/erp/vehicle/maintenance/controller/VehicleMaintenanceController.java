package com.jdend.erp.vehicle.maintenance.controller;

import com.jdend.erp.vehicle.maintenance.dto.VehicleMaintenanceCreateRequest;
import com.jdend.erp.vehicle.maintenance.dto.VehicleMaintenanceResponse;
import com.jdend.erp.vehicle.maintenance.dto.VehicleMaintenanceStatusRowResponse;
import com.jdend.erp.vehicle.maintenance.dto.VehicleMaintenanceVehicleInfoResponse;
import com.jdend.erp.vehicle.maintenance.service.VehicleMaintenanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/vehicle-maintenance")

public class VehicleMaintenanceController {

  private final VehicleMaintenanceService service;

  // 차량관리번호로 차량 정보(차량번호, 검사기간) 조회
  @GetMapping("/vehicle-info")
  public VehicleMaintenanceVehicleInfoResponse vehicleInfo(@RequestParam("mgmtNo") String mgmtNo) {
    return service.getVehicleInfoByMgmtNo(mgmtNo);
  }

  // 정비등록 저장
  @PostMapping
  public VehicleMaintenanceResponse create(@RequestBody VehicleMaintenanceCreateRequest req) {
    return service.create(req);
  }

  // ✅ 정비현황 조회
  // GET /api/vehicle-maintenance/status?vehicleMgmtNo=&vehicleNo=&startDate=2026-01-01&endDate=2026-01-31
  @GetMapping("/status")
  public List<VehicleMaintenanceStatusRowResponse> status(
      @RequestParam(value = "vehicleMgmtNo", required = false) String vehicleMgmtNo,
      @RequestParam(value = "vehicleNo", required = false) String vehicleNo,
      @RequestParam(value = "startDate", required = false) LocalDate startDate,
      @RequestParam(value = "endDate", required = false) LocalDate endDate
  ) {
    return service.searchStatus(vehicleMgmtNo, vehicleNo, startDate, endDate);
  }
}