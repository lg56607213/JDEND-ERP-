package com.jdend.erp.payment.banktx.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "bank_transactions")
public class BankTransaction {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name="bank_name", nullable=false, length=50)
  private String bankName;

  @Column(name="account_no", nullable=false, length=50)
  private String accountNo;

  @Column(name="tx_date", nullable=false)
  private LocalDate txDate;

  @Column(name="deposit_amount", nullable=false)
  private Long depositAmount;

  @Column(name="withdrawal_amount", nullable=false)
  private Long withdrawalAmount;

  @Column(name="summary", length=255)
  private String summary;

  @Column(name="remarks", length=255)
  private String remarks;

  @Column(name="import_batch_id", length=64)
  private String importBatchId;

  @Column(name="row_hash", nullable=false, length=128, unique = true)
  private String rowHash;

  @Column(name="created_at", insertable=false, updatable=false)
  private LocalDateTime createdAt;

  @Column(name="updated_at", insertable=false, updatable=false)
  private LocalDateTime updatedAt;

  @PrePersist
  public void prePersist(){
    if (depositAmount == null) depositAmount = 0L;
    if (withdrawalAmount == null) withdrawalAmount = 0L;
  }
}