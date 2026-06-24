package com.jdend.erp.contract.maturity.repository;

import com.jdend.erp.contract.maturity.entity.MaturityManagement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaturityManagementRepository extends JpaRepository<MaturityManagement, Long> {
  Page<MaturityManagement> findByStatus(String status, Pageable pageable);
}