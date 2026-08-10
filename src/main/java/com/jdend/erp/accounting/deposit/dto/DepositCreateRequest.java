package com.jdend.erp.accounting.deposit.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class DepositCreateRequest {

    /** FK → contracts.id */
    private Long contractId;

    /** "보증금" | "선수금" */
    private String depositType;

    private Long amount;
    private LocalDate transactionDate;
    private String memo;

    /** true이면 전표 자동 생성 */
    private Boolean createVoucher;

    /** 당사 계좌번호 (보통예금 전표 적요에 포함) */
    private String companyAccount;

    /** 지급처 (고객명 또는 직접입력) */
    private String payee;

    /** 지급계좌 (고객 등록 계좌 또는 직접입력) */
    private String payeeAccount;
}
