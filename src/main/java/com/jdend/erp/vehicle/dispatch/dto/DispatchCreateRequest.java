package com.jdend.erp.vehicle.dispatch.dto;

import lombok.*;
import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DispatchCreateRequest {
  public String vehicleMgmtNo;   // 필수
  public String vehicleNo;       // 선택(비우면 차량관리번호로 자동 조회)
  public String dispatchType;    // 필수
  public LocalDate dispatchDate; // 필수

  public String departureAddress;  // 필수
  public String departureContact;  // 필수
  public String arrivalAddress;    // 필수
  public String arrivalContact;    // 필수

  public String remarks;           // 선택
}