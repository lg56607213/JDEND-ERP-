package com.jdend.erp.contract.maturitytermination.controller;

import com.jdend.erp.auth.service.PermissionService;
import com.jdend.erp.contract.maturitytermination.dto.*;
import com.jdend.erp.contract.maturitytermination.service.MaturityTerminationService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/maturity-terminations")
public class MaturityTerminationController {

  private final MaturityTerminationService service;
  private final PermissionService permissionService;

  @GetMapping
  public Page<MaturityTerminationRowDto> list(
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "15") int size
  ) {
    return service.list(status, page, size);
  }

  @GetMapping("/{id}")
  public MaturityTerminationDetailResponse get(@PathVariable Long id) {
    return service.get(id);
  }

  @PostMapping
  public Long create(@RequestBody MaturityTerminationCreateRequest req) {
    return service.create(req);
  }

  @PutMapping("/{id}")
  public void update(@PathVariable Long id, @RequestBody MaturityTerminationUpdateRequest req,
      HttpSession session) {
    permissionService.requireManager(session);
    service.update(id, req);
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id, HttpSession session) {
    permissionService.requireManager(session);
    service.delete(id);
  }
}