package com.jdend.erp.vehicle.fine.repository;

import com.jdend.erp.vehicle.fine.entity.FaxSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FaxSettingRepository extends JpaRepository<FaxSetting, Long> {

  Optional<FaxSetting> findFirstByOrderByIdAsc();
}
