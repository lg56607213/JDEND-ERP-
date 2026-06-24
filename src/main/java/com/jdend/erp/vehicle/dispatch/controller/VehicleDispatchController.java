package com.jdend.erp.vehicle.dispatch.controller;

import com.jdend.erp.vehicle.dispatch.dto.*;
import com.jdend.erp.vehicle.dispatch.service.VehicleDispatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dispatches")

public class VehicleDispatchController {

  private final VehicleDispatchService service;

  @PostMapping
  public DispatchResponse create(@RequestBody DispatchCreateRequest req) {
    return service.create(req);
  }

  // GET /api/dispatches?contractNumber=&vehicleNo=&startDate=YYYY-MM-DD&endDate=YYYY-MM-DD
  @GetMapping
  public List<DispatchResponse> list(
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
  public DispatchResponse detail(@PathVariable Long id) {
    return service.detail(id);
  }

  @PutMapping("/{id}")
  public DispatchResponse update(@PathVariable Long id, @RequestBody DispatchUpdateRequest req) {
    return service.update(id, req);
  }
}