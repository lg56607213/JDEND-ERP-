package com.jdend.erp.partner;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "partners")
public class Partner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="partner_number", nullable = false, unique = true, length = 30)
    private String partnerNumber;

    // 거래처 유형: 개인 / 법인 / 개인사업자 (Customer.customerType 동일 구조)
    @Column(name="customer_type", nullable = false, length = 50)
    private String customerType;

    // 종류: 정비업체 / 수리업체 / 탁송업체 / 기타
    @Column(name="partner_type", length = 50)
    private String partnerType;

    @Column(name="customer_name", nullable = false, length = 100)
    private String customerName;

    @Column(name="registration_number", nullable = false, length = 30)
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

    @CreationTimestamp
    @Column(name="created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name="updated_at")
    private LocalDateTime updatedAt;
}
