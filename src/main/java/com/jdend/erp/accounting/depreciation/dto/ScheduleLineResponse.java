package com.jdend.erp.accounting.depreciation.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleLineResponse {
  public Integer period;
  public String date;
  public Long amount;
  public Long balance;
  public String note;
}