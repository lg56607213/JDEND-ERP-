package com.jdend.erp.vehicle.fine.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 과태료 고지 건. 차량번호 + 위반일시로 당시 계약(임차인)을 찾아 연결한다. */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "traffic_fines")
public class TrafficFine {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "vehicle_no", nullable = false, length = 30)
  private String vehicleNo;

  @Column(name = "vehicle_mgmt_no", length = 20)
  private String vehicleMgmtNo;

  @Column(name = "violation_at", nullable = false)
  private LocalDateTime violationAt;

  /** 위반내용 (속도위반, 신호위반, 주정차위반 등) */
  @Column(name = "violation_type", length = 100)
  private String violationType;

  @Column(name = "violation_place", length = 200)
  private String violationPlace;

  /** 부과기관 (경찰서, 구청 등) */
  @Column(name = "issuing_agency", length = 100)
  private String issuingAgency;

  @Column(name = "amount")
  private Long amount;

  @Column(name = "due_date")
  private LocalDate dueDate;

  @Column(name = "contract_number", length = 30)
  private String contractNumber;

  @Column(name = "customer_name", length = 100)
  private String customerName;

  /** 접수 / 변경요청 / 처리완료 */
  @Column(name = "status", nullable = false, length = 20)
  private String status;

  @Column(name = "remarks", length = 500)
  private String remarks;

  @Column(name = "created_by", length = 100)
  private String createdBy;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @PrePersist
  public void prePersist() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
    if (status == null || status.isBlank()) status = "접수";
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
