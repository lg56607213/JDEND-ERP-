package com.jdend.erp.accounting.depreciation.repository;

import com.jdend.erp.accounting.depreciation.entity.DepreciationPosting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface DepreciationPostingRepository extends JpaRepository<DepreciationPosting, Long> {

  Optional<DepreciationPosting> findByAsset_IdAndBaseMonth(Long assetId, String baseMonth);

  @Query("select count(p) from DepreciationPosting p where p.asset.id = :assetId")
  long countByAssetId(Long assetId);
}