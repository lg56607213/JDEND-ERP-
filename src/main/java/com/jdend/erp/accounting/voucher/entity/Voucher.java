package com.jdend.erp.accounting.voucher.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vouchers")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "voucher_no", nullable = false, length = 50, unique = true)
    private String voucherNo;

    @Column(name = "voucher_date", nullable = false)
    private LocalDate voucherDate;

    @Column(name = "contract_number", length = 50)
    private String contractNumber;

    @Column(name = "vehicle_no", length = 50)
    private String vehicleNo;

    @Column(name = "vehicle_mgmt_no", length = 30)
    private String vehicleMgmtNo;

    @Column(name = "total_amount", nullable = false)
    private Long totalAmount;

    // ✅ 추가: 승인 상태 ("대기" | "승인")
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "memo", length = 255)
    private String memo;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "voucher", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<VoucherLine> lines = new ArrayList<>();

    public void addLine(VoucherLine line) {
        line.setVoucher(this);
        this.lines.add(line);
    }
}