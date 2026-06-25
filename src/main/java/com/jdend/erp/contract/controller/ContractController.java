package com.jdend.erp.contract.controller;

import com.jdend.erp.common.excel.ExcelUploadResultResponse;
import com.jdend.erp.contract.dto.*;
import com.jdend.erp.contract.service.ContractBulkUploadService;
import com.jdend.erp.contract.service.ContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/contracts")

public class ContractController {

  private final ContractService service;
  private final ContractBulkUploadService bulkUploadService;

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
  public ContractResponse update(@PathVariable Long id, @RequestBody ContractRequest req) {
    return service.update(id, req);
  }

  @DeleteMapping("/{id:\\d+}")
  public void delete(@PathVariable Long id) {
    service.delete(id);
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