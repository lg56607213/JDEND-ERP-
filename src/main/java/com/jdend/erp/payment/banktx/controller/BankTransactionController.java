package com.jdend.erp.payment.banktx.controller;

import com.jdend.erp.payment.banktx.dto.*;
import com.jdend.erp.payment.banktx.service.BankTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bank-transactions")
public class BankTransactionController {

  private final BankTransactionService service;

  @GetMapping
  public List<BankTransactionRowResponse> search(
      @RequestParam(value="bank", required=false, defaultValue="") String bank,
      @RequestParam(value="accountNo", required=false, defaultValue="") String accountNo,
      @RequestParam(value="startDate", required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam(value="endDate", required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
  ) {
    return service.search(bank, accountNo, startDate, endDate);
  }

  @GetMapping("/template")
  public ResponseEntity<byte[]> template() {
    byte[] bytes = service.template();
    return ResponseEntity.ok()
        .header("Content-Disposition", "attachment; filename=\"bank_transaction_template.xlsx\"")
        .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        .body(bytes);
  }

  @PostMapping("/upload")
  public UploadResultResponse upload(
      @RequestParam("bankName") String bankName,
      @RequestParam("accountNo") String accountNo,
      @RequestParam("file") MultipartFile file
  ) {
    return service.uploadExcel(bankName, accountNo, file);
  }

  /** 선택 삭제 — 지울 id가 많을 수 있어 본문으로 받는다. */
  @PostMapping("/delete")
  public Map<String, Integer> delete(@RequestBody List<Long> ids) {
    return Map.of("deleted", service.deleteByIds(ids));
  }

  @PatchMapping("/remarks")
  public void updateRemarks(@RequestBody List<RemarksUpdateRequest> list) {
    service.updateRemarksBulk(list);
  }

  @GetMapping("/distinct-accounts")
  public List<BankAccountPickRowResponse> distinctAccounts(
      @RequestParam(value="kw", required=false, defaultValue="") String kw
  ) {
    return service.distinctAccounts(kw);
  }
}
