package com.jdend.erp.contract.service;

import com.jdend.erp.common.excel.ExcelReader;
import com.jdend.erp.common.excel.ExcelTemplateWriter;
import com.jdend.erp.common.excel.ExcelUploadResultResponse;
import com.jdend.erp.contract.dto.ContractRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.jdend.erp.common.excel.ExcelRowParsers.*;

/**
 * 계약 엑셀 일괄 업로드. 행마다 기존 {@link ContractService#create}를 그대로 재사용한다.
 * 이 서비스 자체는 트랜잭션을 걸지 않고, 다른 빈(ContractService)의 @Transactional 메서드를
 * 행마다 호출해 각 행이 독립된 트랜잭션이 되게 한다(한 행 실패가 다른 행에 영향 없음).
 */
@Service
@RequiredArgsConstructor
public class ContractBulkUploadService {

  private static final List<String> HEADERS = List.of(
      "고객번호", "차량번호", "계약구분", "계약유형", "계약시작일", "계약종료일",
      "월대여료", "선수금", "청구횟수", "보증금", "비고"
  );

  private static final List<String> SAMPLE_ROW = List.of(
      "C001", "12가3456", "운용리스", "신규", "2026-01-01", "2027-12-31",
      "500000", "0", "24", "1000000", "샘플 행입니다. 실제 데이터로 바꿔서 업로드하세요."
  );

  private final ContractService contractService;

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
      int rowNumber = i + 2; // 1행=헤더
      try {
        contractService.create(toRequest(rows.get(i)));
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

  private ContractRequest toRequest(Map<String, String> row) {
    return ContractRequest.builder()
        .customerNumber(str(row, "고객번호"))
        .vehicleNo(str(row, "차량번호"))
        .contractType(str(row, "계약구분"))
        .contractCategory(str(row, "계약유형"))
        .startDate(dateVal(row, "계약시작일"))
        .endDate(dateVal(row, "계약종료일"))
        .monthlyRent(longVal(row, "월대여료"))
        .advancePayment(longVal(row, "선수금"))
        .billingCount(intVal(row, "청구횟수"))
        .deposit(longVal(row, "보증금"))
        .remarks(str(row, "비고"))
        .build();
  }
}
