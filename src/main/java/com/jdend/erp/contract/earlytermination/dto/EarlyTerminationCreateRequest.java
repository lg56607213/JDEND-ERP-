package com.jdend.erp.contract.earlytermination.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EarlyTerminationCreateRequest {
  private String contractNumber;

  private String terminationMethod; // 인수/반납
  private LocalDate terminationDate;

  private String status; // 처리대기/처리완료

  private Long terminationAmount;
  private Long uncollectedRent;   // ✅ 직접 입력
  private Long terminationFee;    // 없으면 0 처리
}