package com.jdend.erp.legal.repository;

import com.jdend.erp.legal.entity.LegalProgressEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LegalProgressEntryRepository extends JpaRepository<LegalProgressEntry, Long> {
    List<LegalProgressEntry> findByLegalCaseIdOrderByProgressDateAscIdAsc(Long legalCaseId);
    void deleteByLegalCaseId(Long legalCaseId);
}
