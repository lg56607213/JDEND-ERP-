package com.jdend.erp.payment.banktx.dto;

import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UploadResultResponse {
  private String batchId;
  /** 헤더 아래에서 읽어들인 데이터 행 수 */
  private int parsedRows;
  private int insertedRows;
  /** 이미 등록된 건과 동일해 제외된 행 수 */
  private int skippedDuplicates;
  /** 일자 누락/형식 오류 등으로 등록하지 못한 행 수 */
  private int skippedInvalid;
  /** 인식된 엑셀 헤더 (문제 진단용) */
  private List<String> detectedHeaders;
  /** 등록되지 않은 행의 사유 (최대 20건) */
  private List<String> skipReasons;
}
