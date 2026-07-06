package com.jdend.erp.auth.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginUserAdminResponse {
  private Long id;
  private String loginId;
  private String companyName;
  private String targetDb;
  private String role;
  private Boolean isActive;
  private Boolean taxConsultationEnabled;
}