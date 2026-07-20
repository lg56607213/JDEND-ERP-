package com.jdend.erp.accounting.settings.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdend.erp.accounting.settings.dto.OtherAccountSettingsRequest;
import com.jdend.erp.accounting.settings.dto.OtherAccountSettingsResponse;
import com.jdend.erp.accounting.settings.entity.OtherAccountSettings;
import com.jdend.erp.accounting.settings.repository.OtherAccountSettingsRepository;
import com.jdend.erp.management.financial.repository.FinancialStatementAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OtherAccountSettingsService {

  private final OtherAccountSettingsRepository repo;
  private final FinancialStatementAccountRepository fsAccountRepo;
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

  /** 차입금 개시 차변 계정명 */
  public String getLoanOpenDebitAccount()    { return nested("loanOpenMapping",   "debit");  }
  /** 차입금 개시 대변 계정명 */
  public String getLoanOpenCreditAccount()   { return nested("loanOpenMapping",   "credit"); }
  /** 감가상각 차변 계정명 */
  public String getDeprecDebitAccount()      { return nested("deprecMapping",     "debit");  }
  /** 감가상각 대변 계정명 */
  public String getDeprecCreditAccount()     { return nested("deprecMapping",     "credit"); }
  /** 법적비용 차변 계정명 */
  public String getLegalCostDebitAccount()   { return nested("legalCostMapping",  "debit");  }
  /** 법적비용 대변 계정명 */
  public String getLegalCostCreditAccount()  { return nested("legalCostMapping",  "credit"); }
  /** 차량매각 차변 계정명 */
  public String getSaleDebitAccount()        { return nested("saleMapping",       "debit");  }
  /** 차량매각 대변 계정명 */
  public String getSaleCreditAccount()       { return nested("saleMapping",       "credit"); }
  /** 수납 차변 계정명 */
  public String getPaymentDebitAccount()     { return nested("paymentMapping",    "debit");  }
  /** 수납 대변 계정명 */
  public String getPaymentCreditAccount()    { return nested("paymentMapping",    "credit"); }

  /** 중도해지 미회수렌트료 차변 계정명 */
  public String getEarlyTermUnrealizedRentDebit()  { return nested3("earlyTermMapping","unrealizedRent",   "debit");  }
  /** 중도해지 미회수렌트료 대변 계정명 */
  public String getEarlyTermUnrealizedRentCredit() { return nested3("earlyTermMapping","unrealizedRent",   "credit"); }
  /** 중도해지 수수료 차변 계정명 */
  public String getEarlyTermFeeDebit()             { return nested3("earlyTermMapping","terminationFee",   "debit");  }
  /** 중도해지 수수료 대변 계정명 */
  public String getEarlyTermFeeCredit()            { return nested3("earlyTermMapping","terminationFee",   "credit"); }
  /** 중도해지 상환금액 차변 계정명 */
  public String getEarlyTermAmountDebit()          { return nested3("earlyTermMapping","terminationAmount","debit");  }
  /** 중도해지 상환금액 대변 계정명 */
  public String getEarlyTermAmountCredit()         { return nested3("earlyTermMapping","terminationAmount","credit"); }

  /** 보험 신규/갱신 차변 계정명 */
  public String getInsuranceDebitAccount()         { return nested("insuranceMapping",       "debit");  }
  /** 보험 신규/갱신 대변 계정명 */
  public String getInsuranceCreditAccount()        { return nested("insuranceMapping",       "credit"); }
  /** 보험 변경 환급 차변 계정명 */
  public String getInsuranceRefundDebitAccount()   { return nested("insuranceRefundMapping", "debit");  }
  /** 보험 변경 환급 대변 계정명 */
  public String getInsuranceRefundCreditAccount()  { return nested("insuranceRefundMapping", "credit"); }

  /** 정비 차변 계정명 (공급가액) */
  public String getMaintenanceDebitAccount()        { return nested("maintenanceMapping", "debit");        }
  /** 정비 부가세 차변 계정명 */
  public String getMaintenanceVatDebitAccount()     { return nested("maintenanceMapping", "vatDebit");     }
  /** 정비 대변 계정명 (미지급금 결제) */
  public String getMaintenanceCreditUnpaidAccount() { return nested("maintenanceMapping", "creditUnpaid"); }
  /** 정비 대변 계정명 (법인카드 결제) */
  public String getMaintenanceCreditCardAccount()   { return nested("maintenanceMapping", "creditCard");   }
  /** 정비 대변 계정명 (보통예금 결제) */
  public String getMaintenanceCreditBankAccount()   { return nested("maintenanceMapping", "creditBank");   }
  /** 선급 부가세 차변 계정명 */
  public String getAdvanceVatDebitAccount()         { return nested("advanceVatMapping",  "debit");        }
  /** 매각 감가상각누계액 차변 계정명 */
  public String getSaleAccumDeprecAccount()         { return nested("saleDetailMapping",  "accumDeprec");  }
  /** 매각 미상각잔액 차변 계정명 */
  public String getSaleUndepreciatedAccount()       { return nested("saleDetailMapping",  "undepreciated"); }
  /** 매각 부가세예수금 대변 계정명 */
  public String getSaleVatCreditAccount()           { return nested("saleDetailMapping",  "vatCredit");    }
  /** 매각 차량운반구 대변 계정명 */
  public String getSaleVehicleAssetAccount()        { return nested("saleDetailMapping",  "vehicleAsset"); }

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

  /** earlyTermMapping처럼 section → subKey → key 3단계 구조에서 accountName 반환 */
  @SuppressWarnings("unchecked")
  private String nested3(String section, String subKey, String key) {
    Object sec = getSettingsMap().get(section);
    if (sec instanceof Map) {
      Map<?, ?> sm = (Map<?, ?>) sec;
      Object sub = sm.get(subKey);
      if (sub instanceof Map) {
        Map<?, ?> subm = (Map<?, ?>) sub;
        Object entry = subm.get(key);
        if (entry instanceof Map) {
          Map<?, ?> em = (Map<?, ?>) entry;
          Object name = em.get("accountName");
          if (name instanceof String && !((String) name).isBlank()) return (String) name;
        }
      }
    }
    return null;
  }

  // ────────────────────────────────────────────────────────────────────

  @Transactional
  public OtherAccountSettingsResponse save(OtherAccountSettingsRequest req) {
    // 재무제표관리에 등록된 계정만 저장 허용 (단일 진실: financial_statement_accounts)
    validateAccountCodesExist(req.getSettings());
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

  /**
   * settings 안의 모든 계정코드("account" 키)가 재무제표관리에 실제 존재하는지 검증.
   * 비어있는 값(미지정)은 통과, 존재하지 않는 코드가 하나라도 있으면 저장 거부.
   * (은행계좌 select 값은 "bankAccount" 키라 검증 대상이 아님)
   */
  private void validateAccountCodesExist(Object settings) {
    List<String> invalid = new ArrayList<>();
    collectAndCheckAccountCodes(settings, invalid);
    if (!invalid.isEmpty()) {
      throw new IllegalArgumentException(
          "재무제표관리에 등록되지 않은 계정코드입니다: " + String.join(", ", invalid)
              + " — 재무제표관리에 등록된 계정만 선택할 수 있습니다.");
    }
  }

  private void collectAndCheckAccountCodes(Object node, List<String> invalid) {
    if (node instanceof Map) {
      Map<?, ?> m = (Map<?, ?>) node;
      Object acc = m.get("account");
      if (acc instanceof String) {
        String code = ((String) acc).trim();
        if (!code.isEmpty() && !invalid.contains(code) && !fsAccountRepo.existsByAccountCode(code)) {
          invalid.add(code);
        }
      }
      for (Object v : m.values()) collectAndCheckAccountCodes(v, invalid);
    } else if (node instanceof List) {
      for (Object v : (List<?>) node) collectAndCheckAccountCodes(v, invalid);
    }
  }
}