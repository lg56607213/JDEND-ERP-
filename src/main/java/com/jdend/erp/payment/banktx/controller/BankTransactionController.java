package com.jdend.erp.payment.banktx.controller;

import com.jdend.erp.payment.banktx.dto.*;
import com.jdend.erp.payment.banktx.service.BankTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bank-transactions")

public class BankTransactionController {

  private final BankTransactionService service;

  // ✅ 조회
  // GET /api/bank-transactions?bank=&accountNo=&startDate=2025-01-01&endDate=2025-12-31
  @GetMapping
  public List<BankTransactionRowResponse> search(
      @RequestParam(value="bank", required=false, defaultValue="") String bank,
      @RequestParam(value="accountNo", required=false, defaultValue="") String accountNo,
      @RequestParam(value="startDate", required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam(value="endDate", required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
  ){
    return service.search(bank, accountNo, startDate, endDate);
  }

  // ✅ CSV 업로드
  @PostMapping("/upload")
  public UploadResultResponse upload(@RequestParam("file") MultipartFile file){
    return service.uploadCsv(file);
  }

  // ✅ 돋보기: 계좌 목록(은행/계좌번호 distinct)
  // GET /api/bank-transactions/distinct-accounts?kw=
  @GetMapping("/distinct-accounts")
  public List<BankAccountPickRowResponse> distinctAccounts(
      @RequestParam(value="kw", required=false, defaultValue="") String kw
  ){
    return service.distinctAccounts(kw);
  }
}