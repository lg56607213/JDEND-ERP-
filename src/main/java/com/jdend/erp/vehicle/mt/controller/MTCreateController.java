package com.jdend.erp.vehicle.mt.controller;

import com.jdend.erp.auth.service.PermissionService;
import com.jdend.erp.vehicle.mt.dto.MTCreateRequest;
import com.jdend.erp.vehicle.mt.dto.MTCreateResponse;
import com.jdend.erp.vehicle.mt.service.MTCreateService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mt")

public class MTCreateController {

  private final MTCreateService service;
  private final PermissionService permissionService;

  // POST /api/mt
  @PostMapping
  public MTCreateResponse create(@RequestBody MTCreateRequest req, HttpSession session) {
    permissionService.requireMaintenance(session);
    return service.create(req);
  }
}