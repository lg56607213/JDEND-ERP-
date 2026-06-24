package com.jdend.erp.accounting.settings.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtherAccountSettingsRequest {
  // 프론트에서 보내는 settings 객체(JSON) 그대로 문자열로 받음
  private Object settings;
}