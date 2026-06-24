package com.jdend.erp.contract.earlytermination.repository;

import com.jdend.erp.contract.earlytermination.entity.EarlyTermination;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EarlyTerminationRepository extends JpaRepository<EarlyTermination, Long> {
  Page<EarlyTermination> findByStatus(String status, Pageable pageable);
}