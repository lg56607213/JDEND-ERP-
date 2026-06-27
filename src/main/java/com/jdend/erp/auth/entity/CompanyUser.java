package com.jdend.erp.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** 회사(LoginUser, role=COMPANY) 내부의 직원 로그인 계정. 사용자 아이디는 회사 안에서만 유일하다. */
@Entity
@Table(name = "company_users",
    uniqueConstraints = @UniqueConstraint(name = "uk_company_user", columnNames = {"company_id", "user_login_id"}))
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyUser {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(name = "user_login_id", nullable = false, length = 50)
  private String userLoginId;

  @Column(name = "user_password", nullable = false, length = 100)
  private String userPassword;

  @Column(name = "role", nullable = false, length = 30)
  private String role; // COMPANY_ADMIN / MANAGER / STAFF

  @Column(name = "is_active", nullable = false)
  private Boolean isActive;

  @Column(name = "created_at", insertable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", insertable = false, updatable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  public void prePersist() {
    if (role == null || role.isBlank()) role = "STAFF";
    if (isActive == null) isActive = true;
  }
}
