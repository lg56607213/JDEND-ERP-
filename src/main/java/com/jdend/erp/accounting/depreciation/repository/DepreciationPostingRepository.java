package com.jdend.erp.accounting.depreciation.repository;

import com.jdend.erp.accounting.depreciation.entity.DepreciationPosting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DepreciationPostingRepository extends JpaRepository<DepreciationPosting, Long> {

  Optional<DepreciationPosting> findByAsset_IdAndBaseMonth(Long assetId, String baseMonth);

  @Query("select count(p) from DepreciationPosting p where p.asset.id = :assetId")
  long countByAssetId(Long assetId);

  /** BUG-08: 자산 ID 목록으로 특정 기준월 전표 일괄 조회 (N+1 방지) */
  @Query("select p from DepreciationPosting p where p.asset.id in :assetIds and p.baseMonth = :baseMonth")
  List<DepreciationPosting> findByAssetIdInAndBaseMonth(@Param("assetIds") List<Long> assetIds, @Param("baseMonth") String baseMonth);

  /** BUG-12 수정: 자산별 전표 등록 건수를 한 번에 조회 (N+1 방지) */
  interface AssetCountRow {
    Long getAssetId();
    Long getCnt();
  }

  @Query("select p.asset.id as assetId, count(p) as cnt from DepreciationPosting p where p.asset.id in :assetIds group by p.asset.id")
  List<AssetCountRow> countByAssetIdIn(@Param("assetIds") List<Long> assetIds);
}