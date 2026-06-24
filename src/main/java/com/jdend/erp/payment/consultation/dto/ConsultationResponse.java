package com.jdend.erp.payment.consultation.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ConsultationResponse {
  private Long id;
  private String contractNumber;
  private LocalDate consultDate;
  private String consultContent;
  private LocalDateTime createdAt;
}