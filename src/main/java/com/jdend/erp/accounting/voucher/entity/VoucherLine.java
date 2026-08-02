package com.jdend.erp.accounting.voucher.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "voucher_lines")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id", nullable = false)
    private Voucher voucher;

    @Column(name = "line_type", nullable = false, length = 20)
    private String lineType; // "DEBIT" or "CREDIT"

    @Column(name = "account_code", length = 30)
    private String accountCode;

    @Column(name = "account_name", nullable = false, length = 100)
    private String accountName;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "paid", nullable = false)
    @Builder.Default
    private boolean paid = false;

    @Column(name = "paid_at")
    private java.time.LocalDate paidAt;

    @Column(name = "paid_voucher_no", length = 50)
    private String paidVoucherNo;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}