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

  @PutMapping("/{mgmtNo}/advances")
  public Map<String, Object> saveAll(@PathVariable String mgmtNo, @RequestBody VehicleAdvanceSaveRequest req) {
    int voucherCreatedCount = service.saveAll(mgmtNo, req);

    return Map.of(
      "message", voucherCreatedCount > 0
        ? "선급금 정보가 저장되었습니다. 전표가 발생했습니다."
        : "선급금 정보가 저장되었습니다. 생성된 전표는 없습니다.",
      "voucherCreated", voucherCreatedCount > 0,
      "voucherCreatedCount", voucherCreatedCount
    );
  }
}