package com.jdend.erp.vehicle.mt.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class MTCreateRequest {
  private String contractNumber;   // 화면 표시용
  private String vehicleMgmtNo;    // 필수
  private String vehicleNo;        // 선택
  private String vendor;           // 필수
  private String maintenanceType;  // 필수(정비종류)
  private Long mtCost;             // 필수(비용)
  private LocalDate retrieveDate;  // 필수(회수의뢰일 -> payDate 저장)
}