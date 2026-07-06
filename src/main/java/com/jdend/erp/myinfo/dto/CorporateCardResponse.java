package com.jdend.erp.myinfo.dto;

import com.jdend.erp.myinfo.entity.CorporateCard;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class CorporateCardResponse {
    private Long id;
    private String cardCompany;
    private String cardNumberLast4;
    private String cardHolder;
    private String cardAlias;
    private Boolean isActive;

    public static CorporateCardResponse from(CorporateCard e) {
        return CorporateCardResponse.builder()
                .id(e.getId())
                .cardCompany(e.getCardCompany())
                .cardNumberLast4(e.getCardNumberLast4())
                .cardHolder(e.getCardHolder())
                .cardAlias(e.getCardAlias())
                .isActive(e.getIsActive())
                .build();
    }
}
