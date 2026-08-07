package com.jdend.erp.payment.codef.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "codef_connections")
public class CodefConnection {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // CODEF에서 발급받은 connectedId (인증서 1건 = 1 connectedId)
    @Column(name = "connected_id", nullable = false, length = 100)
    private String connectedId;

    // 은행 기관 코드 (예: 0081=하나, 0004=국민, 0020=우리)
    @Column(name = "organization", nullable = false, length = 10)
    private String organization;

    // 표시용 은행명
    @Column(name = "bank_name", nullable = false, length = 50)
    private String bankName;

    // 계좌번호 (조회용, 선택)
    @Column(name = "account_no", length = 50)
    private String accountNo;

    // B=기업, P=개인
    @Column(name = "client_type", length = 1)
    @Builder.Default
    private String clientType = "B";

    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
