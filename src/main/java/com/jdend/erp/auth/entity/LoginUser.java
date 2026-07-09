package com.jdend.erp.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "login_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginUser {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "login_id", nullable = false, unique = true, length = 50)
  private String loginId;

  @Column(name = "login_password", nullable = false, length = 100)
  private String loginPassword;

  @Column(name = "company_name", nullable = false, length = 100)
  private String companyName;

  @Column(name = "target_db", nullable = false, length = 100)
  private String targetDb;

  @Column(name = "role", nullable = false, length = 30)
  private String role;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive;

  @Column(name = "tax_consultation_enabled", nullable = false)
  private Boolean taxConsultationEnabled;

  @Column(name = "maintenance_enabled", nullable = false)
  private Boolean maintenanceEnabled;

  @Column(name = "created_at", insertable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", insertable = false, updatable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  public void prePersist() {
    if (role == null || role.isBlank()) role = "USER";
    if (isActive == null) isActive = true;
    if (taxConsultationEnabled == null) taxConsultationEnabled = false;
    if (maintenanceEnabled == null) maintenanceEnabled = false;
  }
}