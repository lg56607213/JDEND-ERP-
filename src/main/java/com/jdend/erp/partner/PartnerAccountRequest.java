package com.jdend.erp.partner;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PartnerAccountRequest {
    private String partnerNumber;
    private String bankName;
    private String bankCode;
    private String accountNumber;
    private String accountHolder;
    private String relationship;
    private String registrationNumber;
}
