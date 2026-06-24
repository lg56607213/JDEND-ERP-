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

  private String category;
  private Integer level;
  private Long parentId;
  private String isPostable;
  private String parentName; // 직속 상위 분류명 - 전표등록 select의 optgroup 그룹핑용
}