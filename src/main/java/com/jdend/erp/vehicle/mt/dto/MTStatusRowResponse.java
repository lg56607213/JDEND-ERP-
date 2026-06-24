package com.jdend.erp.vehicle.mt.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MTStatusRowResponse {
  private Long maintenanceId;
  private Long itemId;

  private String vehicleMgmtNo;
  private String vehicleNo;

  private LocalDate contractStart;
  private LocalDate contractEnd;

  private LocalDate retrieveDate;
  private String vendor;
  private String maintenanceType;
  private Long mtCost;
}