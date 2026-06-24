package com.jdend.erp.accounting.depreciation.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepreciationAssetRowResponse {
  public Long id;

  public String vehicleMgmtNo;
  public String vehicleNo;

  public Long acquisitionCost;

  public String depStartDate;
  public String depEndDate;

  public Long monthlyAmount;

  public Long accumulated;
  public Long remaining;

  public String lastDepDate;
  public String voucherDate; // 해당 기준월 전표발생일

  // ✅ 추가
  public Integer totalRounds;        // 총회차
  public Integer postedRounds;       // 기등록회차수
  public Integer currentRound;       // 이번회차
  public boolean currentRoundPosted; // 이번회차 전표등록 여부
}