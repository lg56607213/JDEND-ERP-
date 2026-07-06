package com.jdend.erp.myinfo.dto;

import com.jdend.erp.myinfo.entity.BankAccount;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class BankAccountResponse {
    private Long id;
    private String bankName;
    private String accountNumber;
    private String accountHolder;
    private String accountAlias;
    private Boolean isActive;

    public static BankAccountResponse from(BankAccount e) {
        return BankAccountResponse.builder()
                .id(e.getId())
                .bankName(e.getBankName())
                .accountNumber(e.getAccountNumber())
                .accountHolder(e.getAccountHolder())
                .accountAlias(e.getAccountAlias())
                .isActive(e.getIsActive())
                .build();
    }
}
