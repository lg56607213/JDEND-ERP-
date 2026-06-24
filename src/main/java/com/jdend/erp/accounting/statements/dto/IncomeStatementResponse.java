package com.jdend.erp.accounting.statements.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncomeStatementResponse {
  private Long revenue;               // 매출액
  private Long costOfSales;           // 매출원가

  // 판매비와 관리비
  private Long salary;
  private Long bonus;
  private Long welfare;
  private Long travel;
  private Long entertainment;
  private Long communication;
  private Long utilities;
  private Long taxes;
  private Long depreciation;
  private Long insurance;
  private Long rent;
  private Long vehicleMaintenance;
  private Long fees;

  private Long nonOperatingRevenue;   // 영업외수익
  private Long nonOperatingExpense;   // 영업외비용
  private Long corporateTax;          // 법인세
}