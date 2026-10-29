package com.jdend.erp.web.controller;

import com.jdend.erp.web.dto.WebLinkDtos.*;
import com.jdend.erp.web.service.WebLinkService;
import com.jdend.erp.web.service.WebLinkService.HoldConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 홈페이지 Worker → ERP 서버 간 공개 API (명세: 에이스렌터카 홈페이지 docs/rentcarerp-api.md).
 * 인증과 회사 DB 선택은 TenantFilter 가 Authorization: Bearer 키로 처리한다(세션 없음).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public")
public class PublicWebApiController {

  private final WebLinkService service;

  // GET /api/public/short/availability?from=2026-11-14T10:00&to=2026-11-17T10:00&location=1
  // location 은 현재 지점 구분 없이 받기만 한다(명세 호환).
  @GetMapping("/short/availability")
  public AvailabilityResponse availability(
      @RequestParam String from,
      @RequestParam String to,
      @RequestParam(required = false) String location
  ) {
    return service.availability(from, to);
  }

  // POST /api/public/short/holds
  @PostMapping("/short/holds")
  public HoldResponse createHold(@RequestBody HoldRequest req) {
    try {
      return service.createHold(req);
    } catch (DataIntegrityViolationException e) {
      // 같은 예약번호가 동시에 두 번 들어온 경우: 먼저 저장된 건을 그대로 응답(멱등)
      return service.findHold(req == null ? null : req.getNo()).orElseThrow(() -> e);
    }
  }

  // GET /api/public/vehicles?q=12가
  @GetMapping("/vehicles")
  public VehicleSearchResponse vehicles(@RequestParam(required = false, defaultValue = "") String q) {
    return service.searchVehicles(q);
  }

  @ExceptionHandler(HoldConflictException.class)
  public ResponseEntity<?> handleConflict(HoldConflictException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
  }
}
