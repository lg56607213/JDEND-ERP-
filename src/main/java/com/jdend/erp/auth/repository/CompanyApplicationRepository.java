package com.jdend.erp.auth.repository;

import com.jdend.erp.auth.entity.CompanyApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompanyApplicationRepository extends JpaRepository<CompanyApplication, Long> {

    List<CompanyApplication> findByStatus(String status);

    List<CompanyApplication> findAllByOrderByCreatedAtDesc();
}
