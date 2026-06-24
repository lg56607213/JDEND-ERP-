package com.jdend.erp.vehicle.dto;

import lombok.*;
import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class VehicleOrderRequest {

  public String orderStatus;
  public String makerContractNo;
  public String carModel;
  public String optionName;

  // ✅ 추가: 차량 정보
  public String modelYear;          // 연식
  public String fuelType;           // 유종
  public Integer displacement;      // 배기량
  public LocalDate firstRegDate;    // 최초등록일
  public LocalDate inspectionStart; // 검사유효기간(시작)
  public LocalDate inspectionEnd;   // 검사유효기간(종료)

  public Long vehiclePrice;
  public Long optionPrice;

  public LocalDate orderDate;

  public String chassisNo;
  public Long releasePrice;
  public Long totalAdvancePrice;
  public String vehicleNo;

  public LocalDate registerDate;

  // ✅ 파일 정보(선택)
  public String registerFileName;
  public String registerFilePath;
}
