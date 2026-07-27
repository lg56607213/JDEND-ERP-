package com.jdend.erp.accounting.deposit.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class DepositListItemResponse {

    private Long contractId;
    private String contractNumber;
    private String customerName;
    private String vehicleNo;

    /** "보증금" | "선수금" */
    private String depositType;

    /** 계약서 상의 보증금/선수금 금액 */
    private Long contractDepositAmount;

    /** 총 수납액 */
    private Long totalCollected;

    /** 총 지급(반환)액 */
    private Long totalRefunded;

    /** 보증금잔액 = 총수납액 - 총지급액 (아직 미반환 잔액) */
    private Long balance;

    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate lastTransactionDate;
}
