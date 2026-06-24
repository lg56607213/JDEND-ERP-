package com.jdend.erp.management.financial.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialStatementAccountResponse {
  private Long id;
  private String statementType;
  private String accountCode;
  private String accountName;
  private String accountType;
  private Integer displayOrder;
  private String isActive;
}