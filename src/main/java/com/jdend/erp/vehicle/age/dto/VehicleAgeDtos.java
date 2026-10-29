package com.jdend.erp.vehicle.age.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

public final class VehicleAgeDtos {

  private VehicleAgeDtos() {}

  /** 차령관리 목록 한 줄 (차량 1대) */
  @Getter @Builder
  public static class Row {
    private Long id;
    private String vehicleMgmtNo;
    private String vehicleNo;
    private String carModel;
    private String orderStatus;
    private LocalDate firstRegDate;
    private LocalDate vehicleAgeExpiryDate;
    /** 만료일까지 남은 일수 (지났으면 음수, 미등록이면 null) */
    private Long dday;
    /** 만료 / 만료임박(90일 이내) / 정상 / 미등록 */
    private String status;
  }

  /** 차량 1대 차령만료일 저장 (null 이면 지움) */
  @Getter @Setter @NoArgsConstructor @AllArgsConstructor
  public static class UpdateRequest {
    private LocalDate vehicleAgeExpiryDate;
  }

  /** 일괄 저장 한 줄: 차량관리번호 또는 차량번호로 차량을 찾는다 */
  @Getter @Setter @NoArgsConstructor @AllArgsConstructor
  public static class BulkItem {
    private String vehicleMgmtNo;
    private String vehicleNo;
    private LocalDate vehicleAgeExpiryDate;
  }

  @Getter @Builder
  public static class BulkResult {
    private int total;
    private int updated;
    private int unchanged;
    private List<String> errors;
  }
}
