package com.jdend.erp.accounting.cash.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity(name = "CashBankTransaction") // ✅ 엔티티 이름 충돌 방지 (중요)
@Table(name = "bank_transactions")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankTransaction {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "bank_name", length = 100)
  private String bankName;

  @Column(name = "account_no", length = 100)
  private String accountNo;

  @Column(name = "tx_date")
  private LocalDate txDate;

  @Column(name = "deposit_amount")
  private Long depositAmount;

  @Column(name = "withdrawal_amount")
  private Long withdrawalAmount;

  @Column(name = "summary", length = 255)
  private String summary;

  @Column(name = "remarks", length = 255)
  private String remarks;

  @Column(name = "import_batch_id", length = 100)
  private String importBatchId;

  @Column(name = "row_hash", length = 255)
  private String rowHash;

  @Column(name = "created_at", insertable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", insertable = false, updatable = false)
  private LocalDateTime updatedAt;
}