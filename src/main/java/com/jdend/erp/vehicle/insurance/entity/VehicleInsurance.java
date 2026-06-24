package com.jdend.erp.vehicle.insurance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_insurances")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleInsurance {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "vehicle_order_id")
  private Long vehicleOrderId;

  @Column(name = "vehicle_mgmt_no", nullable = false, length = 20)
  private String vehicleMgmtNo;

  @Column(name = "vehicle_no", length = 30)
  private String vehicleNo;

  @Column(name = "contract_number", length = 50)
  private String contractNumber;

  @Column(name = "insurance_start_date", nullable = false)
  private LocalDate insuranceStartDate;

  @Column(name = "insurance_end_date", nullable = false)
  private LocalDate insuranceEndDate;

  @Column(name = "bodily_injury", length = 50)
  private String bodilyInjury;

  @Column(name = "property_damage", length = 50)
  private String propertyDamage;

  @Column(name = "personal_injury", length = 50)
  private String personalInjury;

  @Column(name = "auto_injury", length = 50)
  private String autoInjury;

  @Column(name = "uninsured", length = 50)
  private String uninsured;

  @Column(name = "own_vehicle", length = 50)
  private String ownVehicle;

  @Column(name = "emergency", length = 50)
  private String emergency;

  @Column(name = "age_range", length = 20)
  private String ageRange;

  @Column(name = "driver_range", length = 30)
  private String driverRange;

  @Column(name = "special_terms", length = 1000)
  private String specialTerms;

  @Column(name = "insurance_premium", nullable = false)
  private Long insurancePremium;

  @Column(name = "created_at", insertable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", insertable = false, updatable = false)
  private LocalDateTime updatedAt;
}