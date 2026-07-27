package com.jdend.erp.accounting.prepaidrent.entity;

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
@Table(name = "prepaid_rents")
public class PrepaidRent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK → contracts.id */
    @Column(name = "contract_id", nullable = false)
    private Long contractId;

    /** "입금" | "적용" */
    @Column(name = "transaction_type", nullable = false, length = 10)
    private String transactionType;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "memo", length = 500)
    private String memo;

    @Builder.Default
    @Column(name = "voucher_created", nullable = false)
    private Boolean voucherCreated = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
