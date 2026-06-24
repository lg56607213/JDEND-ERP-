package com.jdend.erp.vehicle.controller;

import com.jdend.erp.vehicle.dto.VehicleRegisterRequest;
import com.jdend.erp.vehicle.dto.VehicleOrderResponse;
import com.jdend.erp.vehicle.service.VehicleOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/vehicle-register")
public class VehicleRegisterController {

  private final VehicleOrderService service;

  // ✅ 차량관리번호로 조회 (등록화면용)
  // /api/vehicle-register/J0003
  @GetMapping("/{mgmtNo}")
  public ResponseEntity<?> searchForRegister(@PathVariable String mgmtNo) {
    try {
      VehicleOrderResponse res = service.detail(mgmtNo);

      if (!"출고완료".equals(res.getOrderStatus())) {
        return ResponseEntity.badRequest().body(Map.of("message", "출고완료 상태의 차량만 등록할 수 있습니다."));
      }

      return ResponseEntity.ok(res);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
  }

  // ✅ 등록 저장 + 파일 업로드
  // /api/vehicle-register/J0003 (POST multipart)
  @PostMapping(value = "/{mgmtNo}", consumes = "multipart/form-data")
  public ResponseEntity<?> register(
    @PathVariable String mgmtNo,

    @RequestParam String vehicleNo,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate registerDate,

    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inspectionStart,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inspectionEnd,

    @RequestParam(required = false) String modelYear,
    @RequestParam(required = false) String fuelType,
    @RequestParam(required = false) Integer displacement,

    @RequestPart(required = false) MultipartFile file
  ) {
    try {
      VehicleRegisterRequest req = VehicleRegisterRequest.builder()
        .vehicleNo(vehicleNo)
        .registerDate(registerDate)
        .inspectionStart(inspectionStart)
        .inspectionEnd(inspectionEnd)
        .modelYear(modelYear)
        .fuelType(fuelType)
        .displacement(displacement)
        .build();

      service.registerVehicle(mgmtNo, req, file);
      return ResponseEntity.ok(Map.of("result", "ok", "message", "등록 완료"));
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(Map.of("result", "fail", "message", e.getMessage()));
    }
  }
}
