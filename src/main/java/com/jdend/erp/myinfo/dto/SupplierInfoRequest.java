package com.jdend.erp.myinfo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SupplierInfoRequest {
    private String companyName;         // ① 상호명
    private String ceoName;             // ② 대표자명
    private String businessType;        // ③ 업태
    private String businessItem;        // ④ 업종(종목)
    private String email;               // ⑤ 이메일
    private String registrationNumber;  // ⑥ 사업자등록번호
    private String address;             // ⑦ 사업장 주소
}
