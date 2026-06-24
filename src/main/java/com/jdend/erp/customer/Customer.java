package com.jdend.erp.customer;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ✅ 고객번호 (C001 같은 값)
    @Column(name="customer_number", nullable = false, unique = true, length = 30)
    private String customerNumber;

    @Column(name="customer_type", nullable = false, length = 50)
    private String customerType;

    @Column(name="customer_name", nullable = false, length = 100)
    private String customerName;

    @Column(name="registration_number", nullable = false, length = 30, unique = true)
    private String registrationNumber;

    @Column(length = 30)
    private String phone;

    @Column(length = 50)
    private String ceo;

    @Column(name="business_type", length = 50)
    private String businessType;

    @Column(name="business_item", length = 50)
    private String businessItem;

    @Column(length = 255)
    private String address;

    @Column(name="bill_address", length = 255)
    private String billAddress;

    @Column(length = 50)
    private String manager;

    @Column(name="manager_phone", length = 30)
    private String managerPhone;

    @Column(name="manager_email", length = 100)
    private String managerEmail;

    @Column(name="bill_email", length = 100)
    private String billEmail;

    @Column(name="register_date")
    private LocalDate registerDate;

    @Column(name="created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name="updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
