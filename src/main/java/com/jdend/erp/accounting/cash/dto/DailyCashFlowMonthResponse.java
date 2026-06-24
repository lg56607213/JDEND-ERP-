package com.jdend.erp.accounting.cash.dto;

import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyCashFlowMonthResponse {
  private List<DailyCashFlowRowResponse> rows;

  private Long totalBankIncome;
  private Long totalBankExpense;

  private Long totalVoucherIncome;
  private Long totalVoucherExpense;

  private Long totalDiffIncome;
  private Long totalDiffExpense;
}