package com.jdend.erp.accounting.statements.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncomeStatementResponse {
  private StatementNodeResponse revenue;
  private StatementNodeResponse expense;

  private Long totalRevenue;
  private Long totalExpense;
  private Long netIncome;
}
