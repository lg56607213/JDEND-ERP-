package com.jdend.erp.vehicle.fine.controller;

import com.jdend.erp.auth.service.AuthService;
import com.jdend.erp.auth.service.PermissionService;
import com.jdend.erp.vehicle.fine.dto.TrafficFineDtos.ContractMatch;
import com.jdend.erp.vehicle.fine.dto.TrafficFineDtos.Response;
import com.jdend.erp.vehicle.fine.dto.TrafficFineDtos.SaveRequest;
import com.jdend.erp.vehicle.fine.service.TrafficFineService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/traffic-fines")
public class TrafficFineController {

  private final TrafficFineService service;
  private final PermissionService permissionService;

  // GET /api/traffic-fines?vehicleNo=&customerName=&status=&startDate=&endDate=
  @GetMapping
  public List<Response> list(
    @RequestParam(required = false) String vehicleNo,
    @RequestParam(required = false) String customerName,
    @RequestParam(required = false) String status,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
  ) {
    return service.list(vehicleNo, customerName, status, startDate, endDate);
  }

  // GET /api/traffic-fines/contract-match?vehicleNo=&date=YYYY-MM-DD
  @GetMapping("/contract-match")
  public List<ContractMatch> contractMatch(@RequestParam String vehicleNo,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return service.findContracts(vehicleNo, date);
  }

  @GetMapping("/{id}")
  public Response detail(@PathVariable Long id) {
    return service.detail(id);
  }

  @PostMapping
  public Response create(@RequestBody SaveRequest req, HttpSession session) {
    return service.create(req, (String) session.getAttribute(AuthService.SESSION_LOGIN_ID));
  }

  @PutMapping("/{id}")
  public Response update(@PathVariable Long id, @RequestBody SaveRequest req, HttpSession session) {
    permissionService.requireManager(session);
    return service.update(id, req);
  }

  @DeleteMapping("/{id}")
  public Map<String, String> delete(@PathVariable Long id, HttpSession session) {
    permissionService.requireManager(session);
    service.delete(id);
    return Map.of("message", "삭제되었습니다.");
  }

  // GET /api/traffic-fines/{id}/print — 과태료 스캔본 + 계약서 스캔본 합본 PDF
  @GetMapping("/{id}/print")
  public ResponseEntity<byte[]> print(@PathVariable Long id) throws IOException {
    byte[] pdf = service.printPdf(id);
    return ResponseEntity.ok()
      .contentType(MediaType.APPLICATION_PDF)
      .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"traffic-fine-" + id + ".pdf\"")
      .body(pdf);
  }
}
