package com.jdend.erp.accounting.depreciation.controller;

import com.jdend.erp.accounting.depreciation.dto.*;
import com.jdend.erp.accounting.depreciation.entity.DepreciationAsset;
import com.jdend.erp.accounting.depreciation.service.DepreciationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/depreciation")

public class DepreciationController {

  private final DepreciationService service;

  @GetMapping("/assets")
  public List<DepreciationAssetRowResponse> list(
      @RequestParam(required = false) String baseMonth,
      @RequestParam(required = false) String depMethod,
      @RequestParam(required = false) String assetType,
      @RequestParam(required = false) String vehicleNo,
      @RequestParam(required = false) LocalDate depStart,
      @RequestParam(required = false) LocalDate depEnd
  ) {
    return service.listAssets(baseMonth, depMethod, assetType, vehicleNo, depStart, depEnd);
  }

  @PostMapping("/assets")
  public Map<String, Object> create(@RequestBody DepreciationAssetCreateRequest req) {
    Long id = service.createAsset(req);
    return Map.of("id", id);
  }

  @GetMapping("/assets/{id}")
  public DepreciationAsset get(@PathVariable Long id) {
    return service.getAsset(id);
  }

  @GetMapping("/assets/{id}/schedule")
  public List<ScheduleLineResponse> schedule(@PathVariable Long id) {
    return service.getSchedule(id);
  }

  @PostMapping("/assets/{id}/schedule-change")
  public List<ScheduleLineResponse> scheduleChange(@PathVariable Long id, @RequestBody ScheduleChangeRequest req) {
    return service.changeSchedule(id, req);
  }

  @PostMapping("/postings")
  public Map<String, Object> post(@RequestBody PostDepreciationRequest req) {
    Map<String, Object> result = service.postDepreciation(req);
    return result;
  }

  @PostMapping("/complete")
  public Map<String, Object> complete(@RequestBody Map<String, Object> body) {
    @SuppressWarnings("unchecked")
    List<Integer> ids = (List<Integer>) body.get("assetIds");
    List<Long> assetIds = ids.stream().map(Integer::longValue).toList();
    service.completeAssets(assetIds);
    return Map.of("ok", true);
  }
}