package com.jdend.erp.myinfo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CorporateCardRequest {
    private String cardCompany;
    private String cardNumberLast4;
    private String cardHolder;
    private String cardAlias;
    private Boolean isActive;
}
