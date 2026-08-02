package com.jdend.erp.vehicle.dto;

import lombok.*;

import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class VehicleRegisterRequest {
  private String vehicleNo;          // 차량번호(필수)
  private LocalDate registerDate;    // 시스템 등록일자

  // BUG-07 수정: 차량 최초등록일(자동차 등록증 상 최초등록일) — 엔티티 firstRegDate 에 저장
  private LocalDate firstRegDate;

  private LocalDate inspectionStart;
  private LocalDate inspectionEnd;

  private String modelYear;
  private String fuelType;
  private Integer displacement;
}
