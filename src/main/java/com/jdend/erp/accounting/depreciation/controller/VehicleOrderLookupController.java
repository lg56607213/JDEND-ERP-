package com.jdend.erp.accounting.depreciation.controller;

import com.jdend.erp.accounting.depreciation.repository.VehicleOrderLookupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/accounting/depreciation/vehicle-orders")

public class VehicleOrderLookupController {

  private final VehicleOrderLookupRepository repo;

  // 돋보기 검색 (감가상각/회계용)
  // GET /api/accounting/depreciation/vehicle-orders/search?kw=12가
  @GetMapping("/search")
  public List<Map<String, Object>> search(@RequestParam(required = false, defaultValue = "") String kw) {
    return repo.search(kw);
  }
}