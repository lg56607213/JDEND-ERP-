package com.jdend.erp.auth.repository;

import com.jdend.erp.auth.entity.CompanyUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyUserRepository extends JpaRepository<CompanyUser, Long> {

  Optional<CompanyUser> findByCompanyIdAndUserLoginId(Long companyId, String userLoginId);

  boolean existsByCompanyIdAndUserLoginId(Long companyId, String userLoginId);

  // BUG-13-04: 소프트 삭제 후 동일 ID 재사용 허용 — 활성 계정만 중복 체크
  boolean existsByCompanyIdAndUserLoginIdAndIsActive(Long companyId, String userLoginId, boolean isActive);

  List<CompanyUser> findByCompanyIdOrderByIdAsc(Long companyId);

  // BUG-13-05: 사용자 목록에서 비활성화 계정 제외
  List<CompanyUser> findByCompanyIdAndIsActiveTrueOrderByIdAsc(Long companyId);

  long countByCompanyIdAndRole(Long companyId, String role);
}
