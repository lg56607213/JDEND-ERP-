package com.jdend.erp.accounting.monthlyvoucher.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vehicle_orders")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class VehicleOrderMini {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "vehicle_mgmt_no")
  private String vehicleMgmtNo;

  @Column(name = "vehicle_no")
  private String vehicleNo;
}