package com.jdend.erp.vehicle.mt.controller;

import com.jdend.erp.auth.service.PermissionService;
import com.jdend.erp.vehicle.mt.dto.MTStatusRowResponse;
import com.jdend.erp.vehicle.mt.service.MTStatusService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mt")

public class MTStatusController {

  private final MTStatusService service;
  private final PermissionService permissionService;

  // GET /api/mt/status?vehicleMgmtNo=&vehicleNo=&startDate=2026-01-01&endDate=2026-01-31
  @GetMapping("/status")
  public List<MTStatusRowResponse> status(
      @RequestParam(value = "vehicleMgmtNo", required = false) String vehicleMgmtNo,
      @RequestParam(value = "vehicleNo", required = false) String vehicleNo,
      @RequestParam(value = "startDate", required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam(value = "endDate", required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
      HttpSession session
  ) {
    permissionService.requireMaintenance(session);
    return service.search(vehicleMgmtNo, vehicleNo, startDate, endDate);
  }
}