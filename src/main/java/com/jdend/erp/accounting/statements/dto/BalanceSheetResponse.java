package com.jdend.erp.accounting.statements.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceSheetResponse {

  // 자산
  private Long cash;
  private Long rentAssetsStatus;            // 렌트자산현황(총액)
  private Long accumulatedDepreciation;     // 감가상각누계액(마이너스)
  private Long prepaidAssets;               // 선급자산
  private Long vatReceivable;               // 부가세대급금
  private Long rentAssets;                  // rentAssetsStatus + accumulatedDepreciation + prepaidAssets

  // 부채
  private Long borrowedLiabilities;
  private Long advanceReceived;
  private Long accountsPayable;
  private Long deposit;

  // 자본
  private Long capital;
  private Long capitalSurplus;
}