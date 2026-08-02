package com.jdend.erp.payment.consultation.entity;

import com.jdend.erp.contract.entity.Contract;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "consultations")
public class Consultation {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "contract_id")
  private Contract contract;

  @Column(name = "contract_number", nullable = false, length = 30)
  private String contractNumber;

  @Column(name = "consult_date", nullable = false)
  private LocalDate consultDate;

  @Column(name = "consult_content", columnDefinition = "TEXT", nullable = false)
  private String consultContent;

  @CreationTimestamp
  @Column(name="created_at", updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name="updated_at")
  private LocalDateTime updatedAt;
}