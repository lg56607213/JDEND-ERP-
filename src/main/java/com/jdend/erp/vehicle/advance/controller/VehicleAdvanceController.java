package com.jdend.erp.vehicle.advance.controller;

import com.jdend.erp.vehicle.advance.dto.VehicleAdvanceRowDto;
import com.jdend.erp.vehicle.advance.dto.VehicleAdvanceSaveRequest;
import com.jdend.erp.vehicle.advance.service.VehicleAdvanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/vehicle-orders")

public class VehicleAdvanceController {

  private final VehicleAdvanceService service;

  @GetMapping("/{mgmtNo}/advances")
  public List<VehicleAdvanceRowDto> list(@PathVariable String mgmtNo) {
    return service.list(mgmtNo);
  }

  // 발주~선급(pre-실행) 단계 — 행 PK 기반 (…000 공유값 대비)
  @GetMapping("/by-id/{id}/advances")
  public List<VehicleAdvanceRowDto> listById(@PathVariable Long id) {
    return service.listById(id);
  }

  @PutMapping("/by-id/{id}/advances")
  public Map<String, Object> saveAllById(@PathVariable Long id, @RequestBody VehicleAdvanceSaveRequest req) {
    return toResult(service.saveAllById(id, req));
  }

  @PutMapping("/{mgmtNo}/advances")
  public Map<String, Object> saveAll(@PathVariable String mgmtNo, @RequestBody VehicleAdvanceSaveRequest req) {
    return toResult(service.saveAll(mgmtNo, req));
  }

  private Map<String, Object> toResult(int voucherCreatedCount) {
    return Map.of(
      "message", voucherCreatedCount > 0
        ? "선급금 정보가 저장되었습니다. 전표가 발생했습니다."
        : "선급금 정보가 저장되었습니다. 생성된 전표는 없습니다.",
      "voucherCreated", voucherCreatedCount > 0,
      "voucherCreatedCount", voucherCreatedCount
    );
  }
}