package com.jdend.erp.management.financial.dto;

import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialStatementAccountTreeResponse {
  private Long id;
  private String accountCode;
  private String accountName;
  private Integer level;
  private String isActive;
  private String isPostable;
  private boolean leaf;
  private List<FinancialStatementAccountTreeResponse> children;
}
