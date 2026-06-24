package com.jdend.erp.accounting.settings.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "accounting_other_account_settings")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtherAccountSettings {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "settings_json", nullable = false, columnDefinition = "json")
  private String settingsJson; // JSON 문자열로 저장(가장 단순/안전)

  @Column(name = "created_at", insertable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", insertable = false, updatable = false)
  private LocalDateTime updatedAt;
}