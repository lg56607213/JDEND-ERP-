package com.jdend.erp.vehicle.fine.controller;

import com.jdend.erp.auth.service.AuthService;
import com.jdend.erp.auth.service.PermissionService;
import com.jdend.erp.vehicle.fine.dto.TrafficFineDtos.ContractMatch;
import com.jdend.erp.vehicle.fine.dto.TrafficFineDtos.Response;
import com.jdend.erp.vehicle.fine.dto.TrafficFineDtos.SaveRequest;
import com.jdend.erp.vehicle.fine.dto.FineAutomationDtos.BulkUploadResponse;
import com.jdend.erp.vehicle.fine.dto.FineAutomationDtos.FaxLog;
import com.jdend.erp.vehicle.fine.dto.FineAutomationDtos.FaxRequest;
import com.jdend.erp.vehicle.fine.dto.FineAutomationDtos.FaxSettings;
import com.jdend.erp.vehicle.fine.dto.FineAutomationDtos.UploadLog;
import com.jdend.erp.vehicle.fine.service.TrafficFineAutomationService;
import com.jdend.erp.vehicle.fine.service.TrafficFineService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.multipart.MultipartFile;
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
  private final TrafficFineAutomationService automation;
  private final PermissionService permissionService;

  // ── 고지서 일괄 업로드 (파일명 = 차량번호, 내용은 사진에서 자동 인식) ──

  @PostMapping(value = "/bulk-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public BulkUploadResponse bulkUpload(@RequestParam("files") List<MultipartFile> files, HttpSession session) {
    return automation.bulkUpload(files, (String) session.getAttribute(AuthService.SESSION_LOGIN_ID));
  }

  @GetMapping("/uploads")
  public List<UploadLog> uploads(@RequestParam(defaultValue = "300") int limit) {
    return automation.uploads(limit);
  }

  // ── 변경부과 신청 팩스 (고지서 + 계약서 + 임차인 정보) ──

  @GetMapping("/{id}/fax-preview")
  public ResponseEntity<byte[]> faxPreview(@PathVariable Long id) throws IOException {
    return ResponseEntity.ok()
      .contentType(MediaType.APPLICATION_PDF)
      .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"fine-fax-" + id + ".pdf\"")
      .body(automation.faxPdf(id));
  }

  @PostMapping("/{id}/fax")
  public FaxLog sendFax(@PathVariable Long id, @RequestBody FaxRequest req, HttpSession session) throws IOException {
    permissionService.requireManager(session);
    return automation.sendFax(id, req, (String) session.getAttribute(AuthService.SESSION_LOGIN_ID));
  }

  @GetMapping("/{id}/faxes")
  public List<FaxLog> faxesOf(@PathVariable Long id) {
    return automation.faxesOf(id);
  }

  @GetMapping("/faxes")
  public List<FaxLog> faxes(
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
    return automation.faxes(startDate, endDate);
  }

  @GetMapping("/fax-settings")
  public FaxSettings faxSettings() {
    return automation.settings();
  }

  @PutMapping("/fax-settings")
  public FaxSettings saveFaxSettings(@RequestBody FaxSettings req, HttpSession session) {
    permissionService.requireManager(session);
    return automation.saveSettings(req);
  }

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
