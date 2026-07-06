package com.jdend.erp.consultation.repository;

import com.jdend.erp.consultation.entity.TaxConsultation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaxConsultationRepository extends JpaRepository<TaxConsultation, Long> {
    List<TaxConsultation> findByCompanyIdOrderByCreatedAtDesc(Long companyId);
    List<TaxConsultation> findAllByOrderByCreatedAtDesc();
}
