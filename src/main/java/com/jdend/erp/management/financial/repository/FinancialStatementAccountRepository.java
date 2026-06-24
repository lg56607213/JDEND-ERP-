package com.jdend.erp.management.financial.repository;

import com.jdend.erp.management.financial.entity.FinancialStatementAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FinancialStatementAccountRepository extends JpaRepository<FinancialStatementAccount, Long> {
  List<FinancialStatementAccount> findByStatementTypeOrderByDisplayOrderAsc(String statementType);
  boolean existsByStatementTypeAndAccountCode(String statementType, String accountCode);

  List<FinancialStatementAccount> findByCategoryOrderByAccountCodeAsc(String category);
  List<FinancialStatementAccount> findByParentId(Long parentId);
  long countByParentId(Long parentId);
  boolean existsByParentId(Long parentId);
}