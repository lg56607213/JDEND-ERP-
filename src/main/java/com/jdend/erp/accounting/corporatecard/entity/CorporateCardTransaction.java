package com.jdend.erp.accounting.corporatecard.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 법인카드 사용내역 1건.
 *
 * <p>사용일자/금액/적요/카드명은 카드사 엑셀에서 업로드되고,
 * 내역(detail)과 계정분류(accountCode/accountName)는 화면에서 담당자가 입력·선택해 저장한다.
 */
@Entity
@Table(name = "corporate_card_transactions",
        indexes = {
                @Index(name = "idx_ccard_use_date", columnList = "use_date"),
                @Index(name = "idx_ccard_account_code", columnList = "account_code")
        })
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CorporateCardTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 카드 사용일자 (엑셀 업로드) */
    @Column(name = "use_date", nullable = false)
    private LocalDate useDate;

    /** 카드명/카드번호 뒤 4자리 등 (엑셀 업로드, 선택) */
    @Column(name = "card_name", length = 60)
    private String cardName;

    /** 카드사 적요 = 가맹점명 등 (엑셀 업로드) */
    @Column(name = "summary", length = 255)
    private String summary;

    /** 사용금액. 취소·환불 건은 음수로 올릴 수 있다. */
    @Column(name = "amount", nullable = false)
    private Long amount;

    /** 담당자가 화면에서 입력하는 내역(사용 목적/설명) */
    @Column(name = "detail", length = 255)
    private String detail;

    /** 계정분류 — 재무제표관리 계정코드 */
    @Column(name = "account_code", length = 30)
    private String accountCode;

    /** 계정분류 — 계정명(계정코드로 서버에서 채운다) */
    @Column(name = "account_name", length = 100)
    private String accountName;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
