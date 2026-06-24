package com.jdend.erp.vehicle.dto;

import lombok.*;
import java.time.*;
import java.util.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class VehicleOrderResponse {

  public String vehicleMgmtNo;
  public String orderStatus;
  public String makerContractNo;
  public String carModel;
  public String optionName;

  // ✅ 추가: 차량 정보
  public String modelYear;
  public String fuelType;
  public Integer displacement;
  public LocalDate firstRegDate;
  public LocalDate inspectionStart;
  public LocalDate inspectionEnd;

  public Long vehiclePrice;
  public Long optionPrice;
  public Long totalPrice;

  public LocalDate orderDate;
  public String chassisNo;
  public Long releasePrice;
  public Long totalAdvancePrice;
  public String vehicleNo;
  public LocalDate registerDate;

  // ✅ 파일 정보(선택)
  public String registerFileName;
  public String registerFilePath;

  public List<HistoryItem> history;

  @Getter @Setter
  @NoArgsConstructor @AllArgsConstructor
  @Builder
  public static class HistoryItem {
    public String status;
    public LocalDateTime changedAt;
    public String note;
  }
}
