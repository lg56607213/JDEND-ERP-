// src/main/java/com/jdend/erp/vehicle/dto/VehicleLoanVoucherRowResponse.java
package com.jdend.erp.vehicle.dto;

import lombok.*;

import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleLoanVoucherRowResponse {
  private Long id;
  private LocalDate voucherDate;
  private Long amount;
  private String memo;
}