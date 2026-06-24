package com.jdend.erp.accounting.statements.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceSheetResponse {
  private StatementNodeResponse asset;
  private StatementNodeResponse liability;
  private StatementNodeResponse equity;

  private Long totalAsset;
  private Long totalLiability;
  private Long totalEquity;
}
