package com.jdend.erp.vehicle.maintenance.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleMaintenanceItemRequest {

  private String description;
  private Long amount;
  private Long supplyAmount;
  private Long vatAmount;
  private String vendor;
  private LocalDate payDate;
  private String paymentMethod;
  private String paymentDetail;
}