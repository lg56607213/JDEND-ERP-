package com.jdend.erp.vehicle.service;

import com.jdend.erp.common.excel.ExcelReader;
import com.jdend.erp.common.excel.ExcelTemplateWriter;
import com.jdend.erp.common.excel.ExcelUploadResultResponse;
import com.jdend.erp.vehicle.dto.VehicleOrderRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.jdend.erp.common.excel.ExcelRowParsers.*;

/**
 * 차량(발주) 엑셀 일괄 업로드. 행마다 기존 {@link VehicleOrderService#create}를 재사용한다.
 */
@Service
@RequiredArgsConstructor
public class VehicleOrderBulkUploadService {

  private static final List<String> HEADERS = List.of(
      "차종", "차량가격", "옵션가격", "발주일자", "차대번호", "차량번호", "연식", "유종"
  );

  private static final List<String> SAMPLE_ROW = List.of(
      "아반떼", "20000000", "1000000", "2026-01-01", "KMHXX00XXXX000000", "12가3456", "2026", "휘발유"
  );

  private final VehicleOrderService vehicleOrderService;

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
        vehicleOrderService.create(toRequest(rows.get(i)));
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

  private VehicleOrderRequest toRequest(Map<String, String> row) {
    return VehicleOrderRequest.builder()
        .carModel(str(row, "차종"))
        .vehiclePrice(longVal(row, "차량가격"))
        .optionPrice(longVal(row, "옵션가격"))
        .orderDate(dateVal(row, "발주일자"))
        .chassisNo(str(row, "차대번호"))
        .vehicleNo(str(row, "차량번호"))
        .modelYear(str(row, "연식"))
        .fuelType(str(row, "유종"))
        .build();
  }
}
