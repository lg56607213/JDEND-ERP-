package com.jdend.erp.management.financial.service;

import com.jdend.erp.accounting.voucher.repository.VoucherLineRepository;
import com.jdend.erp.management.financial.dto.FinancialStatementAccountRequest;
import com.jdend.erp.management.financial.dto.FinancialStatementAccountResponse;
import com.jdend.erp.management.financial.dto.FinancialStatementVoucherRowResponse;
import com.jdend.erp.management.financial.entity.FinancialStatementAccount;
import com.jdend.erp.management.financial.repository.FinancialStatementAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FinancialStatementAccountService {

  private final FinancialStatementAccountRepository repo;
  private final VoucherLineRepository voucherLineRepository;

  private FinancialStatementAccountResponse toRes(FinancialStatementAccount e) {
    return FinancialStatementAccountResponse.builder()
        .id(e.getId())
        .statementType(e.getStatementType())
        .accountCode(e.getAccountCode())
        .accountName(e.getAccountName())
        .accountType(e.getAccountType())
        .displayOrder(e.getDisplayOrder())
        .isActive(e.getIsActive())
        .build();
  }

  @Transactional(readOnly = true)
  public List<FinancialStatementAccountResponse> list(String statementType) {
    return repo.findByStatementTypeOrderByDisplayOrderAsc(statementType)
        .stream()
        .map(this::toRes)
        .toList();
  }

  @Transactional
  public FinancialStatementAccountResponse create(FinancialStatementAccountRequest req) {
    if (repo.existsByStatementTypeAndAccountCode(req.getStatementType(), req.getAccountCode())) {
      throw new IllegalArgumentException("이미 존재하는 계정코드입니다: " + req.getAccountCode());
    }

    FinancialStatementAccount saved = repo.save(
        FinancialStatementAccount.builder()
            .statementType(req.getStatementType())
            .accountCode(req.getAccountCode())
            .accountName(req.getAccountName())
            .accountType(req.getAccountType())
            .displayOrder(req.getDisplayOrder())
            .isActive(req.getIsActive())
            .build()
    );

    return toRes(saved);
  }

  @Transactional
  public FinancialStatementAccountResponse update(Long id, FinancialStatementAccountRequest req) {
    FinancialStatementAccount e = repo.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 ID: " + id));

    if (!e.getAccountCode().equals(req.getAccountCode())
        && repo.existsByStatementTypeAndAccountCode(req.getStatementType(), req.getAccountCode())) {
      throw new IllegalArgumentException("이미 존재하는 계정코드입니다: " + req.getAccountCode());
    }

    e.setStatementType(req.getStatementType());
    e.setAccountCode(req.getAccountCode());
    e.setAccountName(req.getAccountName());
    e.setAccountType(req.getAccountType());
    e.setDisplayOrder(req.getDisplayOrder());
    e.setIsActive(req.getIsActive());

    return toRes(e);
  }

  @Transactional
  public void delete(Long id) {
    repo.deleteById(id);
  }

  @Transactional(readOnly = true)
  public List<FinancialStatementVoucherRowResponse> getVoucherRows(
      Long accountId,
      LocalDate startDate,
      LocalDate endDate
  ) {
    FinancialStatementAccount account = repo.findById(accountId)
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계정 ID: " + accountId));

    return voucherLineRepository.findVoucherRowsByAccountNameAndDateRange(
        account.getAccountName(),
        startDate,
        endDate
    );
  }
}