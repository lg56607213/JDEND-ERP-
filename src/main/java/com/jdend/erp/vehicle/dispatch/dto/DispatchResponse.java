package com.jdend.erp.vehicle.dispatch.dto;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DispatchResponse {
  public Long id;

  public LocalDate dispatchDate;
  public String dispatchType;

  public String contractNumber;
  public String vehicleMgmtNo;
  public String vehicleNo;

  public String departureAddress;
  public String departureContact;

  public String arrivalAddress;
  public String arrivalContact;

  public String remarks;

  public LocalDateTime createdAt;
  public LocalDateTime updatedAt;
}