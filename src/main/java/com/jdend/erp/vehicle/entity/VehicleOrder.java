package com.jdend.erp.vehicle.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "vehicle_orders")
public class VehicleOrder {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name="vehicle_mgmt_no", nullable=false, unique=true, length=20)
  private String vehicleMgmtNo;

  @Column(name="order_status", nullable=false, length=20)
  private String orderStatus;

  @Column(name="maker_contract_no", length=50)
  private String makerContractNo;

  @Column(name="car_model", nullable=false, length=50)
  private String carModel;

  @Column(name="option_name", length=255)
  private String optionName;

  @Column(name="vehicle_price", nullable=false)
  private Long vehiclePrice;

  @Column(name="option_price", nullable=false)
  private Long optionPrice;

  @Column(name="total_price", nullable=false)
  private Long totalPrice;

  @Column(name="order_date", nullable=false)
  private LocalDate orderDate;

  @Column(name="chassis_no", length=50)
  private String chassisNo;

  @Column(name="release_price")
  private Long releasePrice;

  @Column(name="total_advance_price")
  private Long totalAdvancePrice;

  @Column(name="vehicle_no", length=30)
  private String vehicleNo;

  // ✅ 너 화면에서 "등록일자"로 쓰는 값(업무상 등록일자)
  @Column(name="register_date")
  private LocalDate registerDate;

  // ✅ 추가 입력(검사기간/연식/유종/배기량/최초등록일)
  @Column(name="inspection_start")
  private LocalDate inspectionStart;

  @Column(name="inspection_end")
  private LocalDate inspectionEnd;

  @Column(name="model_year", length=10)
  private String modelYear;

  @Column(name="fuel_type", length=20)
  private String fuelType;

  @Column(name="displacement")
  private Integer displacement;

  // ✅ "최초등록일" (DB에 first_reg_date 컬럼 추가했으니 엔티티도 있어야 함)
  @Column(name="first_reg_date")
  private LocalDate firstRegDate;

  // ✅ 등록증 파일 저장 정보
  @Column(name="register_file_name", length=255)
  private String registerFileName;

  @Column(name="register_file_path", length=500)
  private String registerFilePath;

  @Column(name="created_at")
  private LocalDateTime createdAt;

  @Column(name="updated_at")
  private LocalDateTime updatedAt;

  @PrePersist
  public void prePersist() {
    if (orderStatus == null || orderStatus.isBlank()) orderStatus = "발주전";
    if (vehiclePrice == null) vehiclePrice = 0L;
    if (optionPrice == null) optionPrice = 0L;
    totalPrice = vehiclePrice + optionPrice;

    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  public void preUpdate() {
    if (vehiclePrice == null) vehiclePrice = 0L;
    if (optionPrice == null) optionPrice = 0L;
    totalPrice = vehiclePrice + optionPrice;

    updatedAt = LocalDateTime.now();
  }
}
