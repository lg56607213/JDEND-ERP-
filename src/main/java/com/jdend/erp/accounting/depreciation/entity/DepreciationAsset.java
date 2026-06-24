package com.jdend.erp.accounting.depreciation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "depreciation_assets")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepreciationAsset {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "vehicle_mgmt_no", length = 30)
  private String vehicleMgmtNo;

  @Column(name = "vehicle_no", nullable = false, length = 50, unique = true)
  private String vehicleNo;

  @Column(name = "contract_number", length = 50)
  private String contractNumber;

  @Column(name = "acquisition_cost", nullable = false)
  private Long acquisitionCost;

  @Column(name = "dep_method", length = 20)
  private String depMethod;

  @Column(name = "asset_type", length = 20)
  private String assetType;

  @Column(name = "dep_start_date")
  private LocalDate depStartDate;

  @Column(name = "dep_end_date")
  private LocalDate depEndDate;

  @Column(name = "total_months", nullable = false)
  private Integer totalMonths;

  @Column(name = "monthly_amount", nullable = false)
  private Long monthlyAmount;

  @Column(name = "is_active", nullable = false)
  private Boolean active;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "created_at", insertable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", insertable = false, updatable = false)
  private LocalDateTime updatedAt;
}