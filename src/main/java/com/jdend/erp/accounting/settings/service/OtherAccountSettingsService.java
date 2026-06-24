package com.jdend.erp.accounting.settings.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdend.erp.accounting.settings.dto.OtherAccountSettingsRequest;
import com.jdend.erp.accounting.settings.dto.OtherAccountSettingsResponse;
import com.jdend.erp.accounting.settings.entity.OtherAccountSettings;
import com.jdend.erp.accounting.settings.repository.OtherAccountSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;

@Service
@RequiredArgsConstructor
public class OtherAccountSettingsService {

  private final OtherAccountSettingsRepository repo;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Transactional(readOnly = true)
  public OtherAccountSettingsResponse get() {
    return repo.findLatest()
        .map(s -> {
          try {
            Object obj = objectMapper.readValue(s.getSettingsJson(), Object.class);
            return OtherAccountSettingsResponse.builder().settings(obj).build();
          } catch (Exception e) {
            // 깨졌으면 빈 객체로
            return OtherAccountSettingsResponse.builder().settings(new LinkedHashMap<>()).build();
          }
        })
        .orElseGet(() -> OtherAccountSettingsResponse.builder().settings(new LinkedHashMap<>()).build());
  }

  @Transactional
  public OtherAccountSettingsResponse save(OtherAccountSettingsRequest req) {
    try {
      String json = objectMapper.writeValueAsString(req.getSettings());
      OtherAccountSettings saved = repo.save(
          OtherAccountSettings.builder().settingsJson(json).build()
      );
      Object obj = objectMapper.readValue(saved.getSettingsJson(), Object.class);
      return OtherAccountSettingsResponse.builder().settings(obj).build();
    } catch (Exception e) {
      throw new IllegalArgumentException("설정 저장 JSON 변환 실패: " + e.getMessage(), e);
    }
  }
}