package com.jdend.erp.myinfo.dto;

import com.jdend.erp.myinfo.entity.SupplierInfo;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class SupplierInfoResponse {
    private Long id;
    private String companyName;
    private String ceoName;
    private String businessType;
    private String businessItem;
    private String email;
    private String registrationNumber;
    private String address;

    public static SupplierInfoResponse from(SupplierInfo e) {
        if (e == null) {
            return SupplierInfoResponse.builder().build();
        }
        return SupplierInfoResponse.builder()
                .id(e.getId())
                .companyName(e.getCompanyName())
                .ceoName(e.getCeoName())
                .businessType(e.getBusinessType())
                .businessItem(e.getBusinessItem())
                .email(e.getEmail())
                .registrationNumber(e.getRegistrationNumber())
                .address(e.getAddress())
                .build();
    }
}
