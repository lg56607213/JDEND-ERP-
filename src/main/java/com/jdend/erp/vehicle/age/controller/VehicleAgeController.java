package com.jdend.erp.vehicle.age.controller;

import com.jdend.erp.auth.service.PermissionService;
import com.jdend.erp.vehicle.age.dto.VehicleAgeDtos.BulkItem;
import com.jdend.erp.vehicle.age.dto.VehicleAgeDtos.BulkResult;
import com.jdend.erp.vehicle.age.dto.VehicleAgeDtos.Row;
import com.jdend.erp.vehicle.age.dto.VehicleAgeDtos.UpdateRequest;
import com.jdend.erp.vehicle.age.service.VehicleAgeService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/vehicle-ages")
public class VehicleAgeController {

  private final VehicleAgeService service;
  private final PermissionService permissionService;

  // GET /api/vehicle-ages?keyword=&status=&expiryFrom=&expiryTo=&includeSold=false
  @GetMapping
  public List<Row> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryFrom,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryTo,
      @RequestParam(defaultValue = "false") boolean includeSold
  ) {
    return service.list(keyword, status, expiryFrom, expiryTo, includeSold);
  }

  // PUT /api/vehicle-ages/{id}  { vehicleAgeExpiryDate: "2028-10-07" | null }
  @PutMapping("/{id}")
  public Row update(@PathVariable Long id, @RequestBody UpdateRequest req, HttpSession session) {
    permissionService.requireManager(session);
    return service.update(id, req.getVehicleAgeExpiryDate());
  }

  // POST /api/vehicle-ages/bulk  [ { vehicleMgmtNo | vehicleNo, vehicleAgeExpiryDate } ]
  @PostMapping("/bulk")
  public BulkResult bulk(@RequestBody List<BulkItem> items, HttpSession session) {
    permissionService.requireManager(session);
    return service.bulkUpdate(items);
  }
}
