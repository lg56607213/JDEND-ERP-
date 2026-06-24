package com.jdend.erp.vehicle.advance.dto;

import lombok.*;
import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class VehicleAdvanceRowDto {
  public Long id;              // DB 기존행이면 값 있음, 새행이면 null
  public String itemName;
  public Long advanceAmount;
  public Long supplyAmount;
  public Long vatAmount;
  public LocalDate voucherDate;
  public String paymentMethod;
  public String remark;
}
