package com.jdend.erp.auth.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyUserRequest {
  private String userLoginId;
  private String userPassword;
  private String role; // COMPANY_ADMIN / MANAGER / STAFF
  private Boolean isActive;
}
