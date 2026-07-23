package com.jdend.erp.vehicle.sale.controller;

import com.jdend.erp.auth.service.PermissionService;
import com.jdend.erp.vehicle.sale.dto.VehicleSaleDtos;
import com.jdend.erp.vehicle.sale.service.VehicleSaleService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/vehicle-sales")

public class VehicleSaleController {

  private final VehicleSaleService service;
  private final PermissionService permissionService;

  @PostMapping
  public VehicleSaleDtos.Response create(@RequestBody VehicleSaleDtos.CreateRequest req,
                                          HttpSession session) {
    permissionService.requireManager(session);
    return service.create(req);
  }

  // GET /api/vehicle-sales?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD&buyer=
  @GetMapping
  public List<VehicleSaleDtos.Response> list(
    @RequestParam(required = false) String startDate,
    @RequestParam(required = false) String endDate,
    @RequestParam(required = false) String buyer
  ) {
    LocalDate s = (startDate == null || startDate.isBlank()) ? null : LocalDate.parse(startDate);
    LocalDate e = (endDate == null || endDate.isBlank()) ? null : LocalDate.parse(endDate);
    return service.list(s, e, buyer);
  }

  @GetMapping("/{id}")
  public VehicleSaleDtos.Response detail(@PathVariable Long id) {
    return service.detail(id);
  }

  @PutMapping("/{id}")
  public VehicleSaleDtos.Response update(@PathVariable Long id,
                                          @RequestBody VehicleSaleDtos.UpdateRequest req,
                                          HttpSession session) {
    permissionService.requireManager(session);
    return service.update(id, req);
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id, HttpSession session) {
    permissionService.requireManager(session);
    service.delete(id);
  }
}