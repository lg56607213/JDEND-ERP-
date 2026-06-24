package com.jdend.erp.vehicle.mt.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MTContractSearchRowResponse {
  private String contractNumber;
  private String vehicleNo;
  private String customerName;
  private LocalDate startDate;
  private LocalDate endDate;
  private String vehicleMgmtNo;
}