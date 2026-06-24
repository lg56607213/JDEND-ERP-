package com.jdend.erp.dashboard.dto;

import lombok.*;
import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardMaturityRow {
  private String contractNumber;
  private String customerName;
  private LocalDate endDate;
  private long dday;
}