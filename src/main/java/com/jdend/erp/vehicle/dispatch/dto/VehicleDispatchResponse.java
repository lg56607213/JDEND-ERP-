package com.jdend.erp.vehicle.dispatch.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class VehicleDispatchResponse {
  public Long id;

  public String vehicleMgmtNo;
  public String vehicleNo;

  public String dispatchType;
  public LocalDate dispatchDate;

  public String departureAddress;
  public String departureContact;

  public String arrivalAddress;
  public String arrivalContact;

  public String remarks;

  public LocalDateTime createdAt;
  public LocalDateTime updatedAt;
}