package com.jdend.erp.vehicle.fine.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class TrafficFineDtos {

  private TrafficFineDtos() {}

  /** 등록/수정 요청 */
  public record SaveRequest(
    String vehicleNo,
    LocalDateTime violationAt,
    String violationType,
    String violationPlace,
    String issuingAgency,
    Long amount,
    LocalDate dueDate,
    String contractNumber,
    String status,
    String remarks
  ) {}

  public record Response(
    Long id,
    String vehicleNo,
    String vehicleMgmtNo,
    LocalDateTime violationAt,
    String violationType,
    String violationPlace,
    String issuingAgency,
    Long amount,
    LocalDate dueDate,
    String contractNumber,
    String customerName,
    String status,
    String remarks,
    boolean fineScanned,
    boolean contractScanned,
    LocalDateTime createdAt
  ) {}

  /** 위반일 기준 계약 후보 */
  public record ContractMatch(
    String contractNumber,
    String customerName,
    LocalDate startDate,
    LocalDate endDate,
    LocalDate terminationDate
  ) {}
}
