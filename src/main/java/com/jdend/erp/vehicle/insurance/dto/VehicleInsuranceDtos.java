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

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
  }
}