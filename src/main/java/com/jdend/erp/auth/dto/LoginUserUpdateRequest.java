package com.jdend.erp.auth.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginUserUpdateRequest {
  private String loginPassword;
  private String companyName;
  private String targetDb;
  private Boolean isActive;
}