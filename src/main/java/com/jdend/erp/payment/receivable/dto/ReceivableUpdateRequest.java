package com.jdend.erp.payment.receivable.dto;

import lombok.*;

import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ReceivableUpdateRequest {
  private Long receivableAmount;
  private LocalDate receivableDate;
  private String receivableType;
  private String content;
  private String status;
}