package com.jdend.erp.vehicle.dto;

import lombok.*;
import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class VehicleStateResponse {
  private String vehicleNo;
  private String vehicleMgmtNo;
  private String carModel;
  private Long vehiclePrice;
  private String vehicleState;     // 계약 / 대기 / 매각
  private String contractorName;   // 계약자명 (대기/매각이면 null)
  private LocalDate contractStartDate;
  private LocalDate contractEndDate;
  private LocalDate registeredDate; // 차량등록일 (registerDate or orderDate)
}
