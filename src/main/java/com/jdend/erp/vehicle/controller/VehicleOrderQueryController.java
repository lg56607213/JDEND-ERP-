package com.jdend.erp.vehicle.controller;

import com.jdend.erp.contract.repository.ContractRepository;
import com.jdend.erp.vehicle.dto.VehicleAvailableResponse;
import com.jdend.erp.vehicle.entity.VehicleOrder;
import com.jdend.erp.vehicle.repository.VehicleOrderRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/vehicle-orders")

public class VehicleOrderQueryController {

  private final VehicleOrderRepository vehicleOrderRepo;
  private final ContractRepository contractRepo;

  // ✅ 정비등록(돋보기)용: 차량 검색 (kw 비어도 OK)
  // GET /api/vehicle-orders/search?kw=
  @GetMapping("/search")
  public List<VehicleSearchRow> search(@RequestParam(value = "kw", required = false, defaultValue = "") String kw) {
    String keyword = kw == null ? "" : kw.trim();

    List<VehicleOrder> list = vehicleOrderRepo.searchTop500(keyword);

    List<VehicleSearchRow> out = new ArrayList<>();
    for (VehicleOrder v : list) {
      out.add(new VehicleSearchRow(
          v.getVehicleMgmtNo(),
          v.getVehicleNo(),
          v.getCarModel(),
          v.getInspectionStart(),
          v.getInspectionEnd()
      ));
    }
    return out;
  }

  // ✅ 수정모드 단건 조회 (계약중이어도 조회 가능)
  @GetMapping("/lookup")
  public VehicleAvailableResponse lookup(@RequestParam String vehicleNo) {
    VehicleOrder v = vehicleOrderRepo.findByVehicleNoNormalized(vehicleNo)
      .orElseThrow(() -> new RuntimeException("차량 없음: " + vehicleNo));

    return VehicleAvailableResponse.builder()
      .vehicleNo(v.getVehicleNo())
      .vehicleModel(v.getCarModel())
      .vehicleMgmtNo(v.getVehicleMgmtNo())
      .build();
  }

  // ✅ 등록모드: “미계약 차량만” 검색
  @GetMapping("/available")
  public List<VehicleAvailableResponse> available(@RequestParam(defaultValue = "") String keyword) {
    String kw = keyword.trim();

    List<VehicleOrder> candidates = vehicleOrderRepo.searchTop500(kw);

    List<VehicleAvailableResponse> out = new ArrayList<>();
    for (VehicleOrder v : candidates) {
      String vno = v.getVehicleNo();
      if (vno == null || vno.isBlank()) continue;

      boolean contracted = contractRepo.existsByVehicleNoNormalized(vno);
      if (contracted) continue;

      out.add(VehicleAvailableResponse.builder()
        .vehicleNo(v.getVehicleNo())
        .vehicleModel(v.getCarModel())
        .vehicleMgmtNo(v.getVehicleMgmtNo())
        .build());
    }
    return out;
  }

  @Getter
  @AllArgsConstructor
  public static class VehicleSearchRow {
    private String vehicleMgmtNo;
    private String vehicleNo;
    private String carModel;
    private LocalDate inspectionStart;
    private LocalDate inspectionEnd;
  }
}