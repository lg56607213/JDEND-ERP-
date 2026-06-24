package com.jdend.erp.payment.schedule.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleRowDto {
  private Integer no;

  private String startDate;
  private String endDate;
  private String taxDate;
  private String payDate;

  private Long rent;
  private Long principal;
  private Long interest;
  private Long balance;

  // ✅ 추가
  private Long paid;        // 수납금액
  private Long unpaid;      // 미납금액
  private Long receivable;  // 미수금액
  private String status;    // 완납 / 부분수납 / 미납 / 예정
}