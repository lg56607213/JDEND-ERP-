package com.jdend.erp.payment.schedule.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ScheduleGenerateRequest {
  private String contractNumber;
  private boolean overwrite; // true면 기존 스케줄 삭제 후 재생성
}