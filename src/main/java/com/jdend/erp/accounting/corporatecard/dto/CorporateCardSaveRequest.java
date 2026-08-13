package com.jdend.erp.accounting.corporatecard.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** 화면에서 입력한 내역 + 계정분류를 한 번에 저장한다. */
@Getter @Setter
@NoArgsConstructor
public class CorporateCardSaveRequest {

    private List<Item> items;

    @Getter @Setter
    @NoArgsConstructor
    public static class Item {
        private Long id;
        private String detail;
        /** 계정코드. 빈 값이면 계정분류를 해제한다. */
        private String accountCode;
    }
}
