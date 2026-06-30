package com.jdend.erp.customer;

import com.jdend.erp.common.excel.ExcelReader;
import com.jdend.erp.common.excel.ExcelTemplateWriter;
import com.jdend.erp.common.excel.ExcelUploadResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.jdend.erp.common.excel.ExcelRowParsers.str;

/**
 * 고객 엑셀 일괄 업로드. 고객번호는 항상 자동 채번(CustomerController와 동일한 규칙)한다.
 * 각 행을 별도 트랜잭션으로 저장해 한 행 실패가 다른 행에 영향을 주지 않게 한다.
 */
@Service
@RequiredArgsConstructor
public class CustomerBulkUploadService {

  private static final List<String> HEADERS = List.of(
      "고객구분", "고객명", "사업자등록번호", "전화번호", "대표자",
      "업태", "업종", "주소", "담당자", "담당자전화", "담당자이메일"
  );

  private static final List<String> SAMPLE_ROW = List.of(
      "법인", "주식회사 샘플", "123-45-67890", "02-1234-5678", "홍길동",
      "서비스업", "렌탈업", "서울특별시 강남구", "김담당", "010-1234-5678", "sample@example.com"
  );

  private final CustomerRepository repo;
  private final CustomerNumberGenerator numberGenerator;

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
        createOne(rows.get(i));
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

  public void createOne(Map<String, String> row) {
    String customerType = str(row, "고객구분");
    String customerName = str(row, "고객명");
    String registrationNumber = str(row, "사업자등록번호");

    if (customerType == null) throw new IllegalArgumentException("고객구분은 필수입니다.");
    if (customerName == null) throw new IllegalArgumentException("고객명은 필수입니다.");
    if (registrationNumber == null) throw new IllegalArgumentException("사업자등록번호는 필수입니다.");

    Customer c = Customer.builder()
        .customerNumber(numberGenerator.next())
        .customerType(customerType)
        .customerName(customerName)
        .registrationNumber(registrationNumber)
        .phone(str(row, "전화번호"))
        .ceo(str(row, "대표자"))
        .businessType(str(row, "업태"))
        .businessItem(str(row, "업종"))
        .address(str(row, "주소"))
        .manager(str(row, "담당자"))
        .managerPhone(str(row, "담당자전화"))
        .managerEmail(str(row, "담당자이메일"))
        .registerDate(LocalDate.now())
        .build();

    repo.save(c);
  }
}
