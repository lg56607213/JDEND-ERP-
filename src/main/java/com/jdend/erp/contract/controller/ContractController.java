package com.jdend.erp.contract.controller;

import com.jdend.erp.auth.service.PermissionService;
import com.jdend.erp.common.excel.ExcelExportService;
import com.jdend.erp.common.excel.ExcelUploadResultResponse;
import com.jdend.erp.contract.dto.*;
import com.jdend.erp.contract.service.ContractBulkUploadService;
import com.jdend.erp.contract.service.ContractService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/contracts")

public class ContractController {

  private final ContractService service;
  private final ContractBulkUploadService bulkUploadService;
  private final PermissionService permissionService;
  private final ExcelExportService excelExportService;

  @GetMapping
  public List<ContractResponse> list() {
    return service.list();
  }

  // ✅ 계약번호 미리보기
  @GetMapping("/next-number")
  public NextContractNumberResponse nextNumber() {
    return NextContractNumberResponse.builder()
      .contractNumber(service.nextNumberPreview())
      .build();
  }

  // ✅✅✅ 계약번호로 full 상세 (프론트가 ?id=R00001002 같은 상황일 때 쓰면 됨)
  // GET /api/contracts/by-number/R00001002/full
  @GetMapping("/by-number/{contractNumber}/full")
  public ContractFullResponse detailFullByNumber(@PathVariable String contractNumber) {
    return service.detailFullByNumber(contractNumber);
  }

  // ✅ full 상세(수정/출력용) - 숫자 id만 받음
  @GetMapping("/{id:\\d+}/full")
  public ContractFullResponse detailFull(@PathVariable Long id) {
    return service.detailFull(id);
  }

  // ✅ 숫자만 id로 받게 제한
  @GetMapping("/{id:\\d+}")
  public ContractResponse detail(@PathVariable Long id) {
    return service.detail(id);
  }

  @PostMapping
  public ContractResponse create(@RequestBody ContractRequest req) {
    return service.create(req);
  }

  @PutMapping("/{id:\\d+}")
  public ContractResponse update(@PathVariable Long id, @RequestBody ContractRequest req, HttpSession session) {
    permissionService.requireManager(session);
    return service.update(id, req);
  }

  @DeleteMapping("/{id:\\d+}")
  public void delete(@PathVariable Long id, HttpSession session) {
    permissionService.requireManager(session);
    service.delete(id);
  }

  @GetMapping("/export")
  public ResponseEntity<byte[]> export() {
    String[] headers = {"계약번호", "고객번호", "고객명", "차량번호", "차종",
            "계약유형", "계약구분", "상태", "시작일", "종료일", "청구횟수", "월렌트료"};
    List<Object[]> rows = service.list().stream().map(c -> new Object[]{
            c.getContractNumber(), c.getCustomerNumber(), c.getCustomerName(),
            c.getVehicleNo(), c.getVehicleModel(),
            c.getContractType(), c.getContractCategory(), c.getStatus(),
            c.getStartDate(), c.getEndDate(), c.getBillingCount(), c.getMonthlyRent()
    }).collect(Collectors.toList());
    byte[] data = excelExportService.build("계약목록", headers, rows);
    return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''contracts.xlsx")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(data);
  }

  @GetMapping("/bulk-upload/template")
  public ResponseEntity<byte[]> bulkUploadTemplate() {
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=contract_template.xlsx")
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(bulkUploadService.template());
  }

  @PostMapping("/bulk-upload")
  public ExcelUploadResultResponse bulkUpload(@RequestParam("file") MultipartFile file) {
    return bulkUploadService.upload(file);
  }
}