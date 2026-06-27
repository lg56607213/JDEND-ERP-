package com.jdend.erp.auth.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {
  private String companyLoginId;   // 통합 아이디(운영자가 회사에 발급)
  private String companyPassword;  // 통합 비밀번호
  private String userLoginId;      // 사용자 아이디(회사 내부, PLATFORM_ADMIN 로그인 시 비워둠)
  private String userPassword;     // 사용자 비밀번호
}