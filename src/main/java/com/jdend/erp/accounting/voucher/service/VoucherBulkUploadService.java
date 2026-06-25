package com.jdend.erp.accounting.voucher.service;

import com.jdend.erp.accounting.voucher.dto.VoucherCreateRequest;
import com.jdend.erp.common.excel.ExcelReader;
import com.jdend.erp.common.excel.ExcelTemplateWriter;
import com.jdend.erp.common.excel.ExcelUploadResultResponse;
import com.jdend.erp.management.financial.entity.FinancialStatementAccount;
import com.jdend.erp.management.financial.repository.FinancialStatementAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.jdend.erp.common.excel.ExcelRowParsers.*;

/**
 * 전표 엑셀 일괄 업로드. 한 행 = 차변/대변 1쌍짜리 전표 1건.
 * 계정은 계정명이 아니라 재무제표관리에서 부여된 계정코드(예: 100101)로 지정하며,
 * 행마다 코드 → 계정명으로 변환한 뒤 기존 {@link VoucherService#create}를 그대로 재사용한다.
 */
@Service
@RequiredArgsConstructor
public class VoucherBulkUploadService {

  private static final List<String> HEADERS = List.of(
      "전표일자", "계약번호", "차량번호", "차변계정코드", "차변금액", "대변계정코드", "대변금액", "적요", "메모"
  );

  private static final List<String> SAMPLE_ROW = List.of(
      "2026-06-25", "R00001001", "12가3456", "100101", "1000000", "3001", "1000000", "샘플 적요", "샘플 메모"
  );

  private final VoucherService voucherService;
  private final FinancialStatementAccountRepository accountRepo;

  public byte[] template() {
    return ExcelTemplateWriter.write(HEADERS, SAMPLE_ROW);
  }

  public ExcelUploadResultResponse upload(MultipartFile file) {
    List<Map<String, String>> rows;
    try {
      rows = ExcelReader.readRows(file.getInputStream());
    } catch (Exception e) {
      throw new IllegalArgumentException("엑셀 파일을 읽을 수 없습니다: " + e.getMessage());
    }

    int success = 0;
    List<ExcelUploadResultResponse.RowError> errors = new ArrayList<>();

    for (int i = 0; i < rows.size(); i++) {
      int rowNumber = i + 2;
      try {
        voucherService.create(toRequest(rows.get(i)));
        success++;
      } catch (Exception e) {
        errors.add(ExcelUploadResultResponse.RowError.builder()
            .rowNumber(rowNumber)
            .message(e.getMessage())
            .build());
      }
    }

    return ExcelUploadResultResponse.builder()
        .totalRows(rows.size())
        .successCount(success)
        .failCount(errors.size())
        .errors(errors)
        .build();
  }

  private VoucherCreateRequest toRequest(Map<String, String> row) {
    LocalDate voucherDate = dateVal(row, "전표일자");
    if (voucherDate == null) {
      throw new IllegalArgumentException("전표일자는 필수입니다.");
    }

    String description = str(row, "적요");

    String debitAccountName = resolveAccountName(str(row, "차변계정코드"), "차변계정코드");
    Long debitAmount = longVal(row, "차변금액");
    if (debitAmount == null || debitAmount <= 0) {
      throw new IllegalArgumentException("차변금액은 0보다 커야 합니다.");
    }

    String creditAccountName = resolveAccountName(str(row, "대변계정코드"), "대변계정코드");
    Long creditAmount = longVal(row, "대변금액");
    if (creditAmount == null || creditAmount <= 0) {
      throw new IllegalArgumentException("대변금액은 0보다 커야 합니다.");
    }

    return VoucherCreateRequest.builder()
        .voucherDate(voucherDate)
        .contractNumber(str(row, "계약번호"))
        .vehicleNo(str(row, "차량번호"))
        .memo(str(row, "메모"))
        .debitEntries(List.of(
            VoucherCreateRequest.VoucherLineRequest.builder()
                .account(debitAccountName)
                .amount(debitAmount)
                .description(description)
                .build()
        ))
        .creditEntries(List.of(
            VoucherCreateRequest.VoucherLineRequest.builder()
                .account(creditAccountName)
                .amount(creditAmount)
                .description(description)
                .build()
        ))
        .build();
  }

  private String resolveAccountName(String accountCode, String columnLabel) {
    if (accountCode == null) {
      throw new IllegalArgumentException(columnLabel + "는 필수입니다.");
    }

    FinancialStatementAccount account = accountRepo.findByAccountCode(accountCode)
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계정코드입니다: " + accountCode));

    if (!"사용".equals(account.getIsPostable())) {
      throw new IllegalArgumentException("전표에 사용할 수 없는 계정코드입니다(전기가능=미사용): " + accountCode);
    }

    return account.getAccountName();
  }
}
