package com.jdend.erp.auth.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginUserCreateRequest {
  private String loginId;
  private String loginPassword;
  private String companyName;
  private String targetDb;
  private String role;
  private Boolean isActive;
}