package com.jdend.erp.web.entity;

import com.jdend.erp.vehicle.entity.VehicleOrder;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 홈페이지(단기렌트 예약)에서 들어온 가예약.
 * 가예약/확정 상태인 동안 그 기간에 해당 차량은 홈페이지 가용 대수에서 빠진다.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "web_rental_holds")
public class WebRentalHold {

  public static final String STATUS_HOLD = "가예약";
  public static final String STATUS_CONFIRMED = "확정";
  public static final String STATUS_CANCELED = "취소";

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // 홈페이지 예약번호 (예: S-20261114-0001). 같은 번호 재전송 시 기존 건을 돌려준다.
  @Column(name = "hold_no", nullable = false, unique = true, length = 40)
  private String holdNo;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vehicle_order_id")
  private VehicleOrder vehicleOrder;

  @Column(name = "vehicle_no", length = 30)
  private String vehicleNo;

  @Column(name = "model_code", length = 40)
  private String modelCode;

  @Column(name = "pickup_at", nullable = false)
  private LocalDateTime pickupAt;

  @Column(name = "return_at", nullable = false)
  private LocalDateTime returnAt;

  @Column(name = "location", length = 100)
  private String location;

  @Column(name = "customer_name", length = 50)
  private String customerName;

  @Column(name = "phone", length = 30)
  private String phone;

  @Column(name = "status", nullable = false, length = 10)
  private String status;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @PrePersist
  public void prePersist() {
    if (status == null || status.isBlank()) status = STATUS_HOLD;
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
