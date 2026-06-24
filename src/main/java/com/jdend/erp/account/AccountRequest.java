package com.jdend.erp.account;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AccountRequest {
    private String customerNumber;
    private String bankName;
    private String bankCode;
    private String accountNumber;
    private String accountHolder;
    private String relationship;
    private String registrationNumber;
}
