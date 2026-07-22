package com.jdend.erp.accounting.settings.repository;

import com.jdend.erp.accounting.settings.entity.OtherAccountSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface OtherAccountSettingsRepository extends JpaRepository<OtherAccountSettings, Long> {

  // BUG-10차-01: @Query에 LIMIT 미지정 시 다중 행 → NonUniqueResultException.
  // Spring Data 파생 메서드(findFirst…)는 LIMIT 1을 자동 적용하므로 안전하다.
  Optional<OtherAccountSettings> findFirstByOrderByIdDesc();
}