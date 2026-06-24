package com.jdend.erp.payment.billing.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "customers")
public class Customers {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "customer_number")
  private String customerNumber;

  @Column(name = "customer_name")
  private String customerName;

  @Column(name = "manager_email")
  private String managerEmail;

  @Column(name = "bill_email")
  private String billEmail;
}