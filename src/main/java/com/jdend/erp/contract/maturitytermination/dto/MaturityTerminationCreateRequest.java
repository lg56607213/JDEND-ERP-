package com.jdend.erp.contract.maturitytermination.dto;

import lombok.*;

import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class MaturityTerminationCreateRequest {

  private String contractNumber;      // 돋보기로 선택
  private String terminationMethod;   // 인수/반납
  private LocalDate terminationDate;  // 종료일자
  private Long unpaidAmount;          // 미청구금액
  private String status;              // 종료예정/종료완료
}