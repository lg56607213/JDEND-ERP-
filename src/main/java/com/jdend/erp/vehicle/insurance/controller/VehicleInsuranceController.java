package com.jdend.erp.vehicle.insurance.controller;

import com.jdend.erp.auth.service.PermissionService;
import com.jdend.erp.vehicle.insurance.dto.VehicleInsuranceDtos;
import com.jdend.erp.vehicle.insurance.service.VehicleInsuranceService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/insurances")

public class VehicleInsuranceController {

  private final VehicleInsuranceService service;
  private final PermissionService permissionService;

  @PostMapping
  public VehicleInsuranceDtos.Response create(@RequestBody VehicleInsuranceDtos.CreateRequest req,
                                               HttpSession session) {
    permissionService.requireManager(session);
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
  public VehicleInsuranceDtos.Response update(@PathVariable Long id,
                                               @RequestBody VehicleInsuranceDtos.UpdateRequest req,
                                               HttpSession session) {
    permissionService.requireManager(session);
    return service.update(id, req);
  }

  @PostMapping("/{id}/change")
  public ResponseEntity<Void> change(@PathVariable Long id,
                                     @RequestBody VehicleInsuranceDtos.InsuranceChangeRequest req,
                                     HttpSession session) {
    permissionService.requireManager(session);
    if (req.changeType == null || req.changeType.isBlank()) req.setChangeType("변경");
    service.change(id, req);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/{id}/terminate")
  public ResponseEntity<Void> terminate(@PathVariable Long id,
                                        @RequestBody VehicleInsuranceDtos.InsuranceChangeRequest req,
                                        HttpSession session) {
    permissionService.requireManager(session);
    req.setChangeType("해지");
    service.change(id, req);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/{id}/changes")
  public List<VehicleInsuranceDtos.ChangeResponse> listChanges(@PathVariable Long id) {
    return service.listChanges(id);
  }

  @PostMapping("/{id}/refund")
  public ResponseEntity<Void> refund(@PathVariable Long id,
                                     @RequestBody VehicleInsuranceDtos.InsuranceChangeRequest req,
                                     HttpSession session) {
    permissionService.requireManager(session);
    req.setChangeType("환입");
    service.refund(id, req);
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id, HttpSession session) {
    permissionService.requireManager(session);
    service.delete(id);
    return ResponseEntity.ok().build();
  }
}