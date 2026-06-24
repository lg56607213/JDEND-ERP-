package com.jdend.erp.contract.maturitytermination.repository;

import com.jdend.erp.contract.maturitytermination.entity.MaturityTermination;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaturityTerminationRepository extends JpaRepository<MaturityTermination, Long> {
  Page<MaturityTermination> findByStatus(String status, Pageable pageable);
}