package com.jdend.erp.dashboard.dto;

import lombok.*;
import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardInsuranceRow {
  private String contractNumber;
  private String customerName;
  private String vehicleNo;
  private LocalDate insuranceEndDate;
  private long dday;
}