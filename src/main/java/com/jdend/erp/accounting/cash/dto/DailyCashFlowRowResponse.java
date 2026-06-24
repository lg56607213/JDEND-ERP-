package com.jdend.erp.accounting.cash.dto;

import lombok.*;

import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyCashFlowRowResponse {
  private LocalDate date;

  private Long bankIncome;
  private Long bankExpense;

  private Long voucherIncome;
  private Long voucherExpense;

  private Long diffIncome;
  private Long diffExpense;
}