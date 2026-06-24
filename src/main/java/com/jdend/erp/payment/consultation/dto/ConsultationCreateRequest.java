package com.jdend.erp.payment.consultation.dto;

import lombok.*;

import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ConsultationCreateRequest {
  private String contractNumber;
  private LocalDate consultDate;
  private String consultContent;
}