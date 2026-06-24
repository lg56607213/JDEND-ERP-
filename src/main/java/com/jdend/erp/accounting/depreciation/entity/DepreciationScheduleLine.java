package com.jdend.erp.accounting.depreciation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "depreciation_schedule_lines")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepreciationScheduleLine {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "asset_id", nullable = false)
  private DepreciationAsset asset;

  @Column(name = "version_no", nullable = false)
  private Integer versionNo;

  @Column(name = "period_no", nullable = false)
  private Integer periodNo;

  @Column(name = "depreciation_date")
  private LocalDate depreciationDate;

  @Column(name = "amount", nullable = false)
  private Long amount;

  @Column(name = "balance", nullable = false)
  private Long balance;

  @Column(name = "note", length = 255)
  private String note;

  @Column(name = "created_at", insertable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", insertable = false, updatable = false)
  private LocalDateTime updatedAt;
}