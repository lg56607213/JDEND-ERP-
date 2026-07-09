package com.jdend.erp.vehicle.mt.controller;

import com.jdend.erp.auth.service.PermissionService;
import com.jdend.erp.vehicle.mt.dto.MTContractSearchRowResponse;
import com.jdend.erp.vehicle.mt.service.MTContractQueryService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mt")

public class MTContractController {

  private final MTContractQueryService service;
  private final PermissionService permissionService;

  // GET /api/mt/contracts/search?keyword=
  @GetMapping("/contracts/search")
  public List<MTContractSearchRowResponse> search(
      @RequestParam(value = "keyword", required = false) String keyword,
      HttpSession session
  ) {
    permissionService.requireMaintenance(session);
    return service.search(keyword);
  }
}