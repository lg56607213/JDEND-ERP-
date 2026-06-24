package com.jdend.erp.account;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="customer_id", nullable = false)
    private Long customerId;

    @Column(name="customer_number", nullable = false, length = 30)
    private String customerNumber;

    @Column(name="bank_name", nullable = false, length = 50)
    private String bankName;

    @Column(name="bank_code", length = 10)
    private String bankCode;

    @Column(name="account_number", nullable = false, length = 50)
    private String accountNumber;

    @Column(name="account_holder", nullable = false, length = 50)
    private String accountHolder;

    @Column(length = 30)
    private String relationship;

    @Column(name="registration_number", length = 30)
    private String registrationNumber;

    @Column(name="register_date")
    private LocalDate registerDate;

    @Column(name="created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name="updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
