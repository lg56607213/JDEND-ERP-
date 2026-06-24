package com.jdend.erp.vehicle.sale.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class VehicleSaleDtos {

  @Getter @Setter
  public static class CreateRequest {
    public String vehicleMgmtNo;   // 필수
    public String vehicleNo;       // 선택(비어도 됨)
    public LocalDate saleDate;     // 필수
    public String buyer;           // 필수
    public Long saleAmount;        // 필수
  }

  @Getter @Setter
  public static class UpdateRequest {
    public LocalDate saleDate;     // 필수
    public String buyer;           // 필수
    public Long saleAmount;        // 필수
    public String status;          // 선택(기본 '완료')
  }

  @Builder
  @Getter
  public static class Response {
    private Long id;

    private Long vehicleOrderId;
    private String vehicleMgmtNo;
    private String vehicleNo;
    private String carModel;
    private String chassisNo;

    private LocalDate saleDate;
    private String buyer;

    private Long saleAmount;
    private Long supplyAmount;
    private Long taxAmount;

    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
  }
}