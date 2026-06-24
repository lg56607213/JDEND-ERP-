package com.jdend.erp.accounting.settings.repository;

import com.jdend.erp.accounting.settings.entity.OtherAccountSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface OtherAccountSettingsRepository extends JpaRepository<OtherAccountSettings, Long> {

  @Query("select s from OtherAccountSettings s order by s.id desc")
  Optional<OtherAccountSettings> findLatest();
}