package com.jdend.erp.management.financial.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "financial_statement_accounts",
    uniqueConstraints = @UniqueConstraint(name = "uk_stmt_code", columnNames = {"statement_type", "account_code"}))
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialStatementAccount {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "statement_type", nullable = false, length = 10)
  private String statementType; // bs / is

  @Column(name = "category", nullable = false, length = 20)
  private String category; // ASSET / LIABILITY / EQUITY / REVENUE / EXPENSE

  @Column(name = "level", nullable = false)
  private Integer level; // 1=대분류 2=중분류 3=소분류 4=소소분류

  @Column(name = "parent_id")
  private Long parentId; // 자기참조(단순 컬럼, 트리는 서비스에서 메모리로 조립). null이면 대분류 루트

  @Column(name = "account_code", nullable = false, length = 30)
  private String accountCode;

  @Column(name = "account_name", nullable = false, length = 100)
  private String accountName;

  @Column(name = "account_type", nullable = false, length = 50)
  private String accountType;

  @Column(name = "display_order", nullable = false)
  private Integer displayOrder;

  @Column(name = "is_active", nullable = false, length = 10)
  private String isActive; // 사용/미사용

  @Column(name = "is_postable", nullable = false, length = 10)
  private String isPostable; // 사용/미사용 - 전표(전기)에 직접 쓸 수 있는지

  @Column(name = "created_at", insertable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", insertable = false, updatable = false)
  private LocalDateTime updatedAt;
}