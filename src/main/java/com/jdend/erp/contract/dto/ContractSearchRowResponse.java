package com.jdend.erp.contract.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
public class ContractSearchRowResponse {

  private String contractNumber;   // 계약번호
  private String customerName;     // 고객명
  private String vehicleNo;        // 차량번호

  private String contractType;     // 계약구분
  private LocalDate startDate;     // 시작일
  private LocalDate endDate;       // 종료일

  private Long monthlyRent;        // 월렌트료
  private Long totalRent;          // 총렌트료 (중도상환 화면에서 필요)
}