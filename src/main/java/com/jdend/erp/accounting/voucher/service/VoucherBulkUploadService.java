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
import java.util.*;

import static com.jdend.erp.common.excel.ExcelRowParsers.*;

/**
 * 전표 엑셀 일괄 업로드. 한 행에는 계정 1개와 차변/대변 중 한쪽 금액만 들어가므로, 차변 1줄 + 대변
 * 여러 줄(또는 그 반대)처럼 한 전표가 여러 줄로 나뉠 수 있다. "전표번호" 열이 같은 행들을 하나의 전표로
 * 묶으며, 전표번호를 비워두면 그 행은 혼자 묶여 항상 차변/대변 한쪽만 채워진 미균형 그룹이 되어 오류로
 * 보고된다(즉 모든 전표는 전표번호로 최소 2줄 이상을 묶어야 한다). 묶은 그룹 안에서 차변 합계와 대변
 * 합계가 같으면 정상, 다르면 그 그룹 전체를 오류로 보고한다.
 * 계정은 계정명이 아니라 재무제표관리 계정코드로 지정하며, 행마다 코드 → 계정명으로 변환한 뒤
 * 기존 {@link VoucherService#create}를 그대로 재사용한다.
 */
@Service
@RequiredArgsConstructor
public class VoucherBulkUploadService {

  private static final List<String> HEADERS = List.of(
      "전표번호", "전표일자", "계약번호", "차량번호", "계정코드", "차변금액", "대변금액", "적요", "메모"
  );

  // 부가세가 섞인 매출 1건이 3줄(차변 1 + 대변 2)로 나뉘는 예시 — 같은 전표번호(G001)로 묶인다.
  // 단순히 차변/대변이 1:1인 경우도 행이 2개로 나뉘므로 전표번호(G002)는 필수로 채워야 한다.
  private static final List<List<String>> SAMPLE_ROWS = List.of(
      List.of("G001", "2026-06-25", "R00001001", "12가3456", "100101", "100000", "", "매출대금 입금", ""),
      List.of("G001", "2026-06-25", "R00001001", "12가3456", "400101", "", "90909", "렌트수익", ""),
      List.of("G001", "2026-06-25", "R00001001", "12가3456", "200103", "", "9091", "부가세예수금", ""),
      List.of("G002", "2026-06-25", "R00001002", "11가1111", "100101", "50000", "", "", ""),
      List.of("G002", "2026-06-25", "R00001002", "11가1111", "3001", "", "50000", "", "")
  );

  private final VoucherService voucherService;
  private final FinancialStatementAccountRepository accountRepo;

  public byte[] template() {
    return ExcelTemplateWriter.writeMultiRow(HEADERS, SAMPLE_ROWS);
  }

  public ExcelUploadResultResponse upload(MultipartFile file) {
    List<Map<String, String>> rows;
    try {
      rows = ExcelReader.readRows(file.getInputStream());
    } catch (Exception e) {
      throw new IllegalArgumentException("엑셀 파일을 읽을 수 없습니다: " + e.getMessage());
    }

    // 행 번호(엑셀 기준, 헤더=1행)를 보존하면서 "전표번호"로 그룹핑.
    // 전표번호가 비어있으면 그 행 하나만 들어가는 단독 그룹으로 취급(기존 1행=1전표와 동일하게 동작).
    LinkedHashMap<String, List<Integer>> groups = new LinkedHashMap<>();
    for (int i = 0; i < rows.size(); i++) {
      int rowNumber = i + 2;
      String groupNo = str(rows.get(i), "전표번호");
      String key = (groupNo != null) ? "G:" + groupNo : "R:" + rowNumber;
      groups.computeIfAbsent(key, k -> new ArrayList<>()).add(i);
    }

    int success = 0;
    List<ExcelUploadResultResponse.RowError> errors = new ArrayList<>();

    for (List<Integer> indices : groups.values()) {
      List<Integer> rowNumbers = indices.stream().map(i -> i + 2).toList();
      try {
        voucherService.create(toRequest(rows, indices));
        success += indices.size();
      } catch (Exception e) {
        String rowLabel = rowNumbers.size() == 1
            ? (rowNumbers.get(0) + "행")
            : (rowNumbers.get(0) + "~" + rowNumbers.get(rowNumbers.size() - 1) + "행");

        errors.add(ExcelUploadResultResponse.RowError.builder()
            .rowNumber(rowNumbers.get(0))
            .message(rowLabel + ": " + e.getMessage())
            .build());
      }
    }

    return ExcelUploadResultResponse.builder()
        .totalRows(rows.size())
        .successCount(success)
        .failCount(rows.size() - success)
        .errors(errors)
        .build();
  }

  private VoucherCreateRequest toRequest(List<Map<String, String>> rows, List<Integer> indices) {
    LocalDate voucherDate = null;
    String contractNumber = null;
    String vehicleNo = null;
    String memo = null;

    List<VoucherCreateRequest.VoucherLineRequest> debitEntries = new ArrayList<>();
    List<VoucherCreateRequest.VoucherLineRequest> creditEntries = new ArrayList<>();

    for (int i : indices) {
      Map<String, String> row = rows.get(i);

      LocalDate rowDate = dateVal(row, "전표일자");
      if (rowDate == null) {
        throw new IllegalArgumentException("전표일자는 필수입니다.");
      }
      if (voucherDate == null) {
        voucherDate = rowDate;
        contractNumber = str(row, "계약번호");
        vehicleNo = str(row, "차량번호");
        memo = str(row, "메모");
      } else if (!voucherDate.equals(rowDate)) {
        throw new IllegalArgumentException("같은 전표번호 안에서 전표일자가 서로 다릅니다.");
      }

      String accountName = resolveAccountName(str(row, "계정코드"));
      String description = str(row, "적요");

      Long debitAmount = longVal(row, "차변금액");
      Long creditAmount = longVal(row, "대변금액");

      boolean hasDebit = debitAmount != null && debitAmount > 0;
      boolean hasCredit = creditAmount != null && creditAmount > 0;

      if (hasDebit == hasCredit) {
        throw new IllegalArgumentException("차변금액과 대변금액 중 하나에만 값을 입력해주세요.");
      }

      if (hasDebit) {
        debitEntries.add(VoucherCreateRequest.VoucherLineRequest.builder()
            .account(accountName).amount(debitAmount).description(description).build());
      } else {
        creditEntries.add(VoucherCreateRequest.VoucherLineRequest.builder()
            .account(accountName).amount(creditAmount).description(description).build());
      }
    }

    long debitSum = debitEntries.stream().mapToLong(VoucherCreateRequest.VoucherLineRequest::getAmount).sum();
    long creditSum = creditEntries.stream().mapToLong(VoucherCreateRequest.VoucherLineRequest::getAmount).sum();

    if (debitSum != creditSum) {
      throw new IllegalArgumentException(
          "차변 합계(" + debitSum + ")와 대변 합계(" + creditSum + ")가 일치하지 않습니다.");
    }

    return VoucherCreateRequest.builder()
        .voucherDate(voucherDate)
        .contractNumber(contractNumber)
        .vehicleNo(vehicleNo)
        .memo(memo)
        .debitEntries(debitEntries)
        .creditEntries(creditEntries)
        .build();
  }

  private String resolveAccountName(String accountCode) {
    if (accountCode == null) {
      throw new IllegalArgumentException("계정코드는 필수입니다.");
    }

    FinancialStatementAccount account = accountRepo.findByAccountCode(accountCode)
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계정코드입니다: " + accountCode));

    if (!"사용".equals(account.getIsPostable())) {
      throw new IllegalArgumentException("전표에 사용할 수 없는 계정코드입니다(전기가능=미사용): " + accountCode);
    }

    return account.getAccountName();
  }
}
