package com.jdend.erp.contact;

import lombok.Getter;

@Getter
public class ContactRequest {
    private String companyName;
    private String contactName;
    private String phone;
    private String email;
    private String category;
    private String content;
}
