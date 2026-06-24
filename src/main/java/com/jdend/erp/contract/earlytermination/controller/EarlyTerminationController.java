package com.jdend.erp.contract.earlytermination.controller;

import com.jdend.erp.contract.earlytermination.dto.*;
import com.jdend.erp.contract.earlytermination.service.EarlyTerminationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/early-terminations")
public class EarlyTerminationController {

  private final EarlyTerminationService service;

  @GetMapping
  public Page<EarlyTerminationRowDto> list(
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "15") int size
  ) {
    return service.list(status, page, size);
  }

  @GetMapping("/{id}")
  public EarlyTerminationDetailResponse get(@PathVariable Long id) {
    return service.get(id);
  }

  @PostMapping
  public Long create(@RequestBody EarlyTerminationCreateRequest req) {
    return service.create(req);
  }

  @PutMapping("/{id}")
  public void update(@PathVariable Long id, @RequestBody EarlyTerminationUpdateRequest req) {
    service.update(id, req);
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id) {
    service.delete(id);
  }
}