package com.jdend.erp.legal.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "legal_cases")
public class LegalCase {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contract_number", nullable = false, length = 30)
    private String contractNumber;

    @Column(name = "vehicle_no", length = 30)
    private String vehicleNo;

    @Column(name = "customer_name", length = 100)
    private String customerName;

    @Column(name = "case_type", length = 50)
    private String caseType;

    @Column(name = "case_number", length = 50)
    private String caseNumber;

    @Column(name = "registration_date")
    private LocalDate registrationDate;

    @Column(name = "legal_cost_payment")
    private Long legalCostPayment;

    @Column(name = "legal_cost_refund")
    private Long legalCostRefund;

    @Column(name = "status", nullable = false, length = 10)
    private String status;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
