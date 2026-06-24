package com.jdend.erp.accounting.depreciation.dto;

import lombok.*;

import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepreciationAssetCreateRequest {
  public String vehicleMgmtNo;
  public String vehicleNo;
  public String contractNumber;

  public Long acquisitionCost;

  public String depMethod;
  public String assetType;

  public LocalDate depStartDate;
  public LocalDate depEndDate;

  public LocalDate voucherDate; // 화면 입력값(저장 로직에서 필요하면 활용)
}