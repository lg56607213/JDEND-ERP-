package com.jdend.erp.payment.banktx.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UploadResultResponse {
  private String batchId;
  private int parsedRows;
  private int insertedRows;
  private int skippedDuplicates;
}