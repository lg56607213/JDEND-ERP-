package com.jdend.erp.myinfo.repository;

import com.jdend.erp.myinfo.entity.SupplierInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SupplierInfoRepository extends JpaRepository<SupplierInfo, Long> {
    // 테넌트 DB당 1건만 유지되는 싱글턴 성격 — 가장 먼저 등록된 1건을 사용한다.
    Optional<SupplierInfo> findTopByOrderByIdAsc();
}
