package com.jdend.erp.dashboard.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardContractStatusResponse {
  private long longTerm;
  private long shortTerm;
  private long waiting;
}