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
  /** 전표 등록 여부 (true = 등록됨) */
  public Boolean hasVoucher;
  /** 전표 등록일 (없으면 "") */
  public String voucherDate;
}