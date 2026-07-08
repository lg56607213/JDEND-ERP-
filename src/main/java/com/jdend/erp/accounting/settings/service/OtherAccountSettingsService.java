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
import java.util.List;
import java.util.Map;

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

  // ── 계정명 조회 헬퍼 (다른 서비스에서 사용) ──────────────────────────

  @Transactional(readOnly = true)
  @SuppressWarnings("unchecked")
  public Map<String, Object> getSettingsMap() {
    return repo.findLatest()
        .map(s -> {
          try {
            return (Map<String, Object>) objectMapper.readValue(s.getSettingsJson(), Map.class);
          } catch (Exception e) {
            return new LinkedHashMap<String, Object>();
          }
        })
        .orElseGet(LinkedHashMap::new);
  }

  /** 선급관리 항목명 → 차변 계정명 (설정 없으면 null) */
  @SuppressWarnings("unchecked")
  public String getPrepaidDebitAccount(String itemName) {
    Object list = getSettingsMap().get("prepaidAccounts");
    if (list instanceof List) {
      for (Object item : (List<?>) list) {
        if (item instanceof Map) {
          Map<?, ?> m = (Map<?, ?>) item;
          if (itemName.equals(m.get("name"))) {
            String name = (String) m.get("accountName");
            return (name != null && !name.isBlank()) ? name : null;
          }
        }
      }
    }
    return null;
  }

  /** 차량관리 실행 차변 계정명 */
  public String getVehicleDebitAccount()     { return nested("vehicleMapping",    "debit");  }
  /** 차량관리 실행 대변 계정명 */
  public String getVehicleCreditAccount()    { return nested("vehicleMapping",    "credit"); }
  /** 차입금상환 차변1 계정명 (원금) */
  public String getLoanDebit1Account()       { return nested("loanMapping",       "debit1"); }
  /** 차입금상환 차변2 계정명 (이자) */
  public String getLoanDebit2Account()       { return nested("loanMapping",       "debit2"); }
  /** 차입금상환 대변 계정명 */
  public String getLoanCreditAccount()       { return nested("loanMapping",       "credit"); }
  /** 정기검사 차변 계정명 */
  public String getInspectionDebitAccount()  { return nested("inspectionMapping", "debit");  }
  /** 정기검사 대변 계정명 */
  public String getInspectionCreditAccount() { return nested("inspectionMapping", "credit"); }

  private String nested(String section, String key) {
    Object sec = getSettingsMap().get(section);
    if (sec instanceof Map) {
      Map<?, ?> sm = (Map<?, ?>) sec;
      Object entry = sm.get(key);
      if (entry instanceof Map) {
        Map<?, ?> em = (Map<?, ?>) entry;
        Object name = em.get("accountName");
        if (name instanceof String && !((String) name).isBlank()) return (String) name;
      }
    }
    return null;
  }

  // ────────────────────────────────────────────────────────────────────

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