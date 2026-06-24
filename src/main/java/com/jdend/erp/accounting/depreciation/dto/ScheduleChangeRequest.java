package com.jdend.erp.accounting.depreciation.dto;

import lombok.*;

import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleChangeRequest {
  public String changeType;      // sale | terminate | extend | amount
  public LocalDate changeDate;

  public Long saleAmount;        // sale
  public Long newMonthlyAmount;  // amount
  public Integer extendMonths;   // extend
}