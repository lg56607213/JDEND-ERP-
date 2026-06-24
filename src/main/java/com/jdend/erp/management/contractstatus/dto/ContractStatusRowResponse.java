package com.jdend.erp.management.contractstatus.dto;

import lombok.*;

import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractStatusRowResponse {
  private String vehicleNumber;
  private String contractNumber;
  private String status;         // 장기/단기/대기 (contracts.status 그대로)
  private String customerName;
  private LocalDate contractStart;
  private LocalDate contractEnd;
  private Long monthlyRent;
  private Long totalRent;
  private Long receivable;       // 간단 계산(없으면 0)
  private Long advance;          // contracts.advance_payment
  private String vehicleType;    // contracts.vehicle_model
}