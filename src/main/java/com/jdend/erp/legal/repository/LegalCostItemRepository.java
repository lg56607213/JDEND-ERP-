package com.jdend.erp.legal.repository;

import com.jdend.erp.legal.entity.LegalCostItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LegalCostItemRepository extends JpaRepository<LegalCostItem, Long> {
    List<LegalCostItem> findByLegalCaseIdOrderByCostDateAscIdAsc(Long legalCaseId);
    void deleteByLegalCaseId(Long legalCaseId);
}
