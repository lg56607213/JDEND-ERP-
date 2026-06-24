package com.jdend.erp.contract.maturity.dto;

import lombok.*;

import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class MaturityUpdateRequest {

  private LocalDate newStartDate;
  private LocalDate newEndDate;
  private Long newMonthlyRent;
  private String status;
}