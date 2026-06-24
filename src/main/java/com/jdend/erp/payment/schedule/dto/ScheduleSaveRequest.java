package com.jdend.erp.payment.schedule.dto;

import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ScheduleSaveRequest {
  private String contractNumber;
  private List<ScheduleRowDto> schedule;
}