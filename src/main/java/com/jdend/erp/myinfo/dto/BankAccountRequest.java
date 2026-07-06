package com.jdend.erp.myinfo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class BankAccountRequest {
    private String bankName;
    private String accountNumber;
    private String accountHolder;
    private String accountAlias;
    private Boolean isActive;
}
