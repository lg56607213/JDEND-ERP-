package com.jdend.erp.accounting.voucher.entity;

import jakarta.persistence.*;
import lombok.*;

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

    @Column(name = "account_name", nullable = false, length = 100)
    private String accountName;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}