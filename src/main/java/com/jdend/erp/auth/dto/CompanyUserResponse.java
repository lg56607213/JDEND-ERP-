package com.jdend.erp.auth.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyUserResponse {
  private Long id;
  private String userLoginId;
  private String role;
  private Boolean isActive;
}
