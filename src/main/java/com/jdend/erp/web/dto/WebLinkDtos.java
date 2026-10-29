package com.jdend.erp.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

/**
 * 홈페이지 연동 DTO.
 * Public* 는 홈페이지 Worker 와의 계약(docs/rentcarerp-api.md, snake_case) — 필드명 바꾸지 말 것.
 * 나머지는 ERP 내부 '홈페이지 연동' 화면용.
 */
public final class WebLinkDtos {

  private WebLinkDtos() {}

  // ───────── 공개 API (홈페이지 → ERP) ─────────

  @Getter @Builder
  public static class AvailableVehicle {
    private String id;
    private String plate;
    @JsonProperty("model_code") private String modelCode;
    @JsonProperty("model_name") private String modelName;
    private Integer year;
  }

  @Getter @AllArgsConstructor
  public static class AvailabilityResponse {
    private List<AvailableVehicle> vehicles;
  }

  @Getter @Setter @NoArgsConstructor
  public static class HoldRequest {
    private String no;
    @JsonProperty("model_code") private String modelCode;
    @JsonProperty("pickup_at") private String pickupAt;
    @JsonProperty("return_at") private String returnAt;
    private String location;
    private String name;
    private String phone;
  }

  @Getter @Builder
  public static class HoldResponse {
    private boolean ok;
    @JsonProperty("hold_id") private Long holdId;
    @JsonProperty("vehicle_id") private String vehicleId;
    private String plate;
  }

  @Getter @Builder
  public static class VehicleInfo {
    private String id;
    private String plate;
    private String name;
    @JsonProperty("class") private String vehicleClass;
    private Integer year;
    @JsonProperty("first_reg") private String firstReg;
    private Long km;
    private String fuel;
    private String color;
    private Integer seats;
    private String options;
  }

  @Getter @AllArgsConstructor
  public static class VehicleSearchResponse {
    private List<VehicleInfo> vehicles;
  }

  // ───────── ERP 내부 화면 (세션 인증) ─────────

  @Getter @Builder
  public static class ShortVehicleRow {
    private Long id;
    private String vehicleNo;
    private String carModel;
    private String modelYear;
    private String orderStatus;
    private String operationType;
    private boolean shortTermUse;
    private String webModelCode;
  }

  @Getter @Setter @NoArgsConstructor
  public static class ShortVehicleUpdateRequest {
    private Boolean shortTermUse;
    private String webModelCode;
  }

  @Getter @Builder
  public static class HoldRow {
    private Long id;
    private String holdNo;
    private Long vehicleOrderId;
    private String vehicleNo;
    private String modelCode;
    private String carModel;
    private String pickupAt;
    private String returnAt;
    private String location;
    private String customerName;
    private String phone;
    private String status;
    private String createdAt;
  }

  @Getter @Setter @NoArgsConstructor
  public static class HoldStatusRequest {
    private String status;
  }
}
