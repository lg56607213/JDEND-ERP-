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

  private String category; // ASSET/LIABILITY/EQUITY/REVENUE/EXPENSE - 대분류 신규 등록시에만 사용
  private Long parentId;   // 상위 분류 id - 신규 노드 등록(createNode)시 필수, null이면 대분류
  private String isPostable; // 사용/미사용 - 전표에서 선택 가능 여부
}