package com.jdend.erp.vehicle.inspection.controller;

import com.jdend.erp.vehicle.inspection.dto.*;
import com.jdend.erp.vehicle.inspection.service.VehicleInspectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/vehicle-inspections")

public class VehicleInspectionController {

  private final VehicleInspectionService service;

  // 등록
  @PostMapping
  public VehicleInspectionResponse create(@RequestBody VehicleInspectionCreateRequest req) {
    return service.create(req);
  }

  // 조회(목록)
  // GET /api/vehicle-inspections?vehicleMgmtNo=&vehicleNo=&validStartFrom=&validEndTo=
  @GetMapping
  public List<VehicleInspectionRowResponse> search(
      @RequestParam(required = false) String vehicleMgmtNo,
      @RequestParam(required = false) String vehicleNo,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate validStartFrom,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate validEndTo
  ) {
    return service.search(vehicleMgmtNo, vehicleNo, validStartFrom, validEndTo);
  }
}