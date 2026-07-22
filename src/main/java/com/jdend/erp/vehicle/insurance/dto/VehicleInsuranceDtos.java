package com.jdend.erp.vehicle.insurance.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class VehicleInsuranceDtos {

  @Getter @Setter
  public static class CreateRequest {
    public String vehicleMgmtNo;          // 필수
    public String vehicleNo;              // 선택

    public LocalDate insuranceStartDate;  // 필수
    public LocalDate insuranceEndDate;    // 필수

    public String bodilyInjury;
    public String propertyDamage;
    public String personalInjury;
    public String autoInjury;
    public String uninsured;
    public String ownVehicle;
    public String emergency;

    public String ageRange;
    public String driverRange;

    public String specialTerms;

    public Long insurancePremium;         // 필수
    public LocalDate voucherDate;         // 전표발생일자 (null이면 insuranceStartDate 사용)
  }

  @Getter @Setter
  public static class InsuranceChangeRequest {
    public String changeType;      // "변경" or "해지"
    public String changeReason;    // 변경사유
    public Long additionalPremium; // 추가납부보험료 (DEBIT: 보험료, CREDIT: 미지급금(보험료))
    public Long refundPremium;     // 환급보험료 (DEBIT: 미수금(보험료), CREDIT: 보험료)
    public LocalDate voucherDate;  // 전표발생일자
  }

  @Getter @Setter
  public static class UpdateRequest {
    public LocalDate insuranceStartDate;  // 필수
    public LocalDate insuranceEndDate;    // 필수

    public String bodilyInjury;
    public String propertyDamage;
    public String personalInjury;
    public String autoInjury;
    public String uninsured;
    public String ownVehicle;
    public String emergency;

    public String ageRange;
    public String driverRange;

    public String specialTerms;

    public Long insurancePremium;         // 필수
  }

  @Getter @Setter
  public static class InsuranceRefundRequest {
    public Long refundPremium;
    public String changeReason;
    public LocalDate voucherDate;
    public void setChangeType(String t) {}
  }

  @Builder
  @Getter
  public static class ChangeResponse {
    private Long id;
    private Long insuranceId;
    private String changeType;
    private String changeReason;
    private Long additionalPremium;
    private Long refundPremium;
    private LocalDate voucherDate;
    private java.time.LocalDateTime createdAt;
  }

  @Builder
  @Getter
  public static class Response {
    private Long id;

    private Long vehicleOrderId;
    private String vehicleMgmtNo;
    private String vehicleNo;
    private String contractNumber;

    private LocalDate insuranceStartDate;
    private LocalDate insuranceEndDate;

    private String bodilyInjury;
    private String propertyDamage;
    private String personalInjury;
    private String autoInjury;
    private String uninsured;
    private String ownVehicle;
    private String emergency;

    private String ageRange;
    private String driverRange;

    private String specialTerms;

    private Long insurancePremium;

    private String voucherNo;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
  }
}