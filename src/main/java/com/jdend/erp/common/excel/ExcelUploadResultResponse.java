package com.jdend.erp.common.excel;

import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExcelUploadResultResponse {
  private int totalRows;
  private int successCount;
  private int failCount;
  private List<RowError> errors;

  @Getter @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class RowError {
    private int rowNumber;
    private String message;
  }
}
