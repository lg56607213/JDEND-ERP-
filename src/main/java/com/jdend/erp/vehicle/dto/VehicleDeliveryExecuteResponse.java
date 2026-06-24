package com.jdend.erp.vehicle.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class VehicleDeliveryExecuteResponse {
  public String vehicleMgmtNo;
  public String orderStatus; // 실행완료
  public Long loanId;        // vehicle_loans.id
}
