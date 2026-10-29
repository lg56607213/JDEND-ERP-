package com.jdend.erp.web.controller;

import com.jdend.erp.auth.service.PermissionService;
import com.jdend.erp.web.dto.WebLinkDtos.*;
import com.jdend.erp.web.service.WebLinkService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** ERP '홈페이지 연동' 화면용 (세션 로그인, 회사 DB 라우팅은 TenantFilter 기본 경로) */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/web-link")
public class WebLinkController {

  private final WebLinkService service;
  private final PermissionService permissionService;

  // GET /api/web-link/vehicles?keyword=
  @GetMapping("/vehicles")
  public List<ShortVehicleRow> vehicles(@RequestParam(required = false) String keyword) {
    return service.listVehicles(keyword);
  }

  // PUT /api/web-link/vehicles/{id}  { shortTermUse: true, webModelCode: "AVANTE_CN7" }
  @PutMapping("/vehicles/{id}")
  public ShortVehicleRow updateVehicle(@PathVariable Long id, @RequestBody ShortVehicleUpdateRequest req,
                                       HttpSession session) {
    permissionService.requireManager(session);
    return service.updateVehicle(id, req);
  }

  // GET /api/web-link/holds?status=&includePast=false
  @GetMapping("/holds")
  public List<HoldRow> holds(@RequestParam(required = false) String status,
                             @RequestParam(defaultValue = "false") boolean includePast) {
    return service.listHolds(status, includePast);
  }

  // PUT /api/web-link/holds/{id}/status  { status: "확정" | "취소" }
  @PutMapping("/holds/{id}/status")
  public HoldRow changeStatus(@PathVariable Long id, @RequestBody HoldStatusRequest req, HttpSession session) {
    permissionService.requireManager(session);
    return service.changeHoldStatus(id, req.getStatus());
  }
}
