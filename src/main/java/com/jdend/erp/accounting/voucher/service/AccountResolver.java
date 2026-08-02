package com.jdend.erp.accounting.voucher.service;

import com.jdend.erp.management.financial.entity.FinancialStatementAccount;
import com.jdend.erp.management.financial.repository.FinancialStatementAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 전표 라인의 계정코드/계정명을 확정하는 공용 리졸버.
 *
 * <p>재무제표 집계({@code StatementAggRepository})는 <b>계정코드</b>로만 합산하고
 * {@code accountCode is not null} 조건으로 걸러내므로, 전표를 만드는 모든 경로는
 * 반드시 계정코드를 채워야 한다. 계정코드가 비면 전표는 저장·승인되지만
 * 재무제표에는 전혀 나타나지 않는다.
 *
 * <p>회계 계정의 기준은 계정명이 아니라 <b>계정코드</b>다.
 * 코드가 주어지면 코드를 신뢰하고 이름은 마스터의 현재 이름으로 덮어쓴다.
 * (사용자가 재무제표관리에서 계정명을 바꿔도 매핑이 깨지지 않는다.)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountResolver {

  private final FinancialStatementAccountRepository accountRepo;

  /** 확정된 계정코드·계정명 */
  public record Resolved(String code, String name) {}

  /**
   * @param code 계정코드 (우선 사용)
   * @param name 계정명 (코드가 없을 때만 사용하는 fallback)
   */
  public Resolved resolve(String code, String name) {
    if (isNotBlank(code)) {
      String c = code.trim();
      return accountRepo.findByAccountCode(c)
          .map(a -> new Resolved(a.getAccountCode(), a.getAccountName()))
          .orElseGet(() -> {
            log.warn("계정코드 '{}' 가 재무제표관리에 없습니다. 재무제표 집계에서 누락될 수 있습니다.", c);
            return new Resolved(c, isNotBlank(name) ? name.trim() : c);
          });
    }

    if (!isNotBlank(name)) return new Resolved(null, null);

    String n = name.trim();
    String resolvedCode = accountRepo.findFirstByAccountName(n)
        .map(FinancialStatementAccount::getAccountCode)
        .orElse(null);

    if (resolvedCode == null) {
      log.warn("계정명 '{}' 에 해당하는 계정이 재무제표관리에 없어 계정코드를 확정하지 못했습니다. "
          + "이 전표 라인은 재무제표에 반영되지 않습니다.", n);
    }
    return new Resolved(resolvedCode, n);
  }

  /** 계정명만 아는 경로(자동전표)에서 계정코드만 필요할 때 */
  public String codeOf(String name) {
    return resolve(null, name).code();
  }

  private static boolean isNotBlank(String s) {
    return s != null && !s.trim().isEmpty();
  }
}
