package com.jdend.erp.vehicle.dto;

import lombok.*;

import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class VehicleRegisterRequest {
  private String vehicleNo;          // 차량번호(필수)
  private LocalDate registerDate;    // 최초등록일(필수)

  private LocalDate inspectionStart;
  private LocalDate inspectionEnd;

  private String modelYear;
  private String fuelType;
  private Integer displacement;
}
