package com.jdend.erp.accounting.payable.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class PayableBulkPayRequest {
    private List<Long> lineIds;
    private LocalDate payDate;
    private String bankAccount;
    private String companyAccount; // 당사 출금 계좌
    private String memo;
    private String payeeName;      // 지급처 이름
    private String paymentAccount; // 지급계좌 정보
}
