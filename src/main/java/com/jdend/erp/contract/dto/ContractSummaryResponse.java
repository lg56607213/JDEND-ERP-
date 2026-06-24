package com.jdend.erp.contract.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractSummaryResponse {
  private String contractNumber;
  private String vehicleNo;

  private String customerName;
  private String registrationNumber;
  private String email;

  private Long monthlyRent;
  private String contractStatus; // contracts.status
}