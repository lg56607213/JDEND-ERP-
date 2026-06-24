package com.jdend.erp.contract.maturity.controller;

import com.jdend.erp.contract.maturity.dto.*;
import com.jdend.erp.contract.maturity.service.MaturityManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/maturity-managements")
public class MaturityManagementController {

  private final MaturityManagementService service;

  @GetMapping
  public Page<MaturityRowDto> list(
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "15") int size
  ) {
    return service.list(status, page, size);
  }

  @GetMapping("/{id}")
  public MaturityDetailResponse get(@PathVariable Long id) {
    return service.get(id);
  }

  @PostMapping
  public Long create(@RequestBody MaturityCreateRequest req) {
    return service.create(req);
  }

  @PutMapping("/{id}")
  public void update(@PathVariable Long id, @RequestBody MaturityUpdateRequest req) {
    service.update(id, req);
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id) {
    service.delete(id);
  }
}