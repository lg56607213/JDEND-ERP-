package com.jdend.erp.vehicle.insurance.controller;

import com.jdend.erp.vehicle.insurance.dto.VehicleInsuranceDtos;
import com.jdend.erp.vehicle.insurance.service.VehicleInsuranceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/insurances")

public class VehicleInsuranceController {

  private final VehicleInsuranceService service;

  @PostMapping
  public VehicleInsuranceDtos.Response create(@RequestBody VehicleInsuranceDtos.CreateRequest req) {
    return service.create(req);
  }

  // GET /api/insurances?contractNumber=&vehicleNo=&startDate=YYYY-MM-DD&endDate=YYYY-MM-DD
  @GetMapping
  public List<VehicleInsuranceDtos.Response> list(
    @RequestParam(required = false) String contractNumber,
    @RequestParam(required = false) String vehicleNo,
    @RequestParam(required = false) String startDate,
    @RequestParam(required = false) String endDate
  ) {
    LocalDate s = (startDate == null || startDate.isBlank()) ? null : LocalDate.parse(startDate);
    LocalDate e = (endDate == null || endDate.isBlank()) ? null : LocalDate.parse(endDate);
    return service.list(contractNumber, vehicleNo, s, e);
  }

  @GetMapping("/{id}")
  public VehicleInsuranceDtos.Response detail(@PathVariable Long id) {
    return service.detail(id);
  }

  @PutMapping("/{id}")
  public VehicleInsuranceDtos.Response update(@PathVariable Long id, @RequestBody VehicleInsuranceDtos.UpdateRequest req) {
    return service.update(id, req);
  }
}