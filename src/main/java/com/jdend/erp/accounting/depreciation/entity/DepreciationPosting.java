package com.jdend.erp.accounting.depreciation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "depreciation_postings",
  uniqueConstraints = @UniqueConstraint(name="uq_dep_posting_asset_month", columnNames={"asset_id","base_month"})
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepreciationPosting {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "asset_id", nullable = false)
  private DepreciationAsset asset;

  @Column(name = "base_month", nullable = false, length = 7)
  private String baseMonth; // YYYY-MM

  @Column(name = "voucher_date", nullable = false)
  private LocalDate voucherDate;

  @Column(name = "created_at", insertable = false, updatable = false)
  private LocalDateTime createdAt;
}