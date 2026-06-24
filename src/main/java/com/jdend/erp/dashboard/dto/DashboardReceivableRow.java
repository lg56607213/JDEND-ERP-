package com.jdend.erp.dashboard.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardReceivableRow {
  private String contractNumber;
  private String customerName;
  private long receivableAmount;
  private long overdueDays;
}