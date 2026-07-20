package com.jdend.erp.payment.receivable.controller;

import com.jdend.erp.auth.service.PermissionService;
import com.jdend.erp.payment.receivable.dto.*;
import com.jdend.erp.payment.receivable.service.ReceivableService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/receivables")

public class ReceivableController {

  private final ReceivableService service;
  private final PermissionService permissionService;

  @PostMapping
  public ReceivableResponse create(@RequestBody ReceivableCreateRequest req) {
    return service.create(req);
  }

  // ✅ 조회 (현황 화면 필터)
  // GET /api/receivables?startDate=2025-01-01&endDate=2025-01-31&customerName=홍&status=미납
  @GetMapping
  public List<ReceivableResponse> list(
      @RequestParam(value="startDate", required=false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

      @RequestParam(value="endDate", required=false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,

      @RequestParam(value="customerName", required=false, defaultValue="") String customerName,

      @RequestParam(value="status", required=false, defaultValue="") String status
  ) {
    return service.list(startDate, endDate, customerName, status);
  }

  @GetMapping("/{id}")
  public ReceivableResponse detail(@PathVariable Long id) {
    return service.detail(id);
  }

  @PutMapping("/{id}")
  public ReceivableResponse update(@PathVariable Long id, @RequestBody ReceivableUpdateRequest req,
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