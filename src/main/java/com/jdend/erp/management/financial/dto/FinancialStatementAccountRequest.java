package com.jdend.erp.management.financial.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialStatementAccountRequest {
  private String statementType; // bs/is
  private String accountCode;
  private String accountName;
  private String accountType;
  private Integer displayOrder;
  private String isActive; // 사용/미사용
}