package com.jdend.erp.report;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReportEmailSettingsRepository extends JpaRepository<ReportEmailSettings, Long> {

    Optional<ReportEmailSettings> findByCompanyId(Long companyId);

    List<ReportEmailSettings> findByEnabledTrue();
}
