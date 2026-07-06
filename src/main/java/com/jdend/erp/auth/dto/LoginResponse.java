package com.jdend.erp.auth.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
  private boolean success;
  private String loginId;
  private String companyName;
  private String role;
  private String message;
  private Boolean taxConsultationEnabled;
}