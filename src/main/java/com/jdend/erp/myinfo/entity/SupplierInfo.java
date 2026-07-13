package com.jdend.erp.myinfo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 세금계산서 공급자(자사) 정보 — 회사(테넌트)별 DB에 1건만 존재하는 싱글턴 성격의 테이블.
 * 전자세금계산서 엑셀 생성 시 공급자 필드(상호/대표자/업태/업종/이메일/사업자번호/주소)의 원천이 된다.
 */
@Entity
@Table(name = "my_supplier_info")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class SupplierInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ① 상호명 */
    @Column(name = "company_name", length = 100)
    private String companyName;

    /** ② 대표자명 */
    @Column(name = "ceo_name", length = 50)
    private String ceoName;

    /** ③ 업태 */
    @Column(name = "business_type", length = 100)
    private String businessType;

    /** ④ 업종(종목) */
    @Column(name = "business_item", length = 100)
    private String businessItem;

    /** ⑤ 이메일 */
    @Column(name = "email", length = 100)
    private String email;

    /** ⑥ 사업자등록번호 (세금계산서 법적 필수) */
    @Column(name = "registration_number", length = 20)
    private String registrationNumber;

    /** ⑦ 사업장 주소 (세금계산서 법적 필수) */
    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
