package com.jdend.erp.myinfo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "my_corporate_cards")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CorporateCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_company", nullable = false, length = 50)
    private String cardCompany;

    @Column(name = "card_number_last4", length = 10)
    private String cardNumberLast4;

    @Column(name = "card_holder", length = 50)
    private String cardHolder;

    @Column(name = "card_alias", length = 50)
    private String cardAlias;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
