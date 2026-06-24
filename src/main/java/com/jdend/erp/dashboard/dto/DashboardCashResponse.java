package com.jdend.erp.dashboard.dto;

import lombok.*;
import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardCashResponse {
  private LocalDate baseDate;
  private long openingBalance;
  private long todayDeposit;
  private long todayWithdrawal;
  private long closingBalance;
}