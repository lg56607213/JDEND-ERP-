package com.jdend.erp.legal.repository;

import com.jdend.erp.legal.entity.LegalCase;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LegalCaseRepository extends JpaRepository<LegalCase, Long> {
    List<LegalCase> findByContractNumberOrderByIdDesc(String contractNumber);
}
