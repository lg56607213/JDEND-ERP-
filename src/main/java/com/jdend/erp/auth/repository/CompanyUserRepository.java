package com.jdend.erp.auth.repository;

import com.jdend.erp.auth.entity.CompanyUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyUserRepository extends JpaRepository<CompanyUser, Long> {

  Optional<CompanyUser> findByCompanyIdAndUserLoginId(Long companyId, String userLoginId);

  boolean existsByCompanyIdAndUserLoginId(Long companyId, String userLoginId);

  List<CompanyUser> findByCompanyIdOrderByIdAsc(Long companyId);

  long countByCompanyIdAndRole(Long companyId, String role);
}
