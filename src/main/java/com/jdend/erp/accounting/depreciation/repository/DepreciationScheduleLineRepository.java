package com.jdend.erp.accounting.depreciation.repository;

import com.jdend.erp.accounting.depreciation.entity.DepreciationScheduleLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DepreciationScheduleLineRepository extends JpaRepository<DepreciationScheduleLine, Long> {

  @Query("select coalesce(max(l.versionNo), 0) from DepreciationScheduleLine l where l.asset.id = :assetId")
  int findMaxVersion(Long assetId);

  /** BUG-12 수정: 자산 목록 N+1 방지 — 자산 ID 목록의 최대 버전을 한 번에 조회 */
  interface MaxVersionRow {
    Long getAssetId();
    Integer getMaxVer();
  }

  @Query("select l.asset.id as assetId, coalesce(max(l.versionNo), 0) as maxVer from DepreciationScheduleLine l where l.asset.id in :assetIds group by l.asset.id")
  List<MaxVersionRow> findMaxVersionsByAssetIds(@Param("assetIds") List<Long> assetIds);

  /** BUG-12 수정: 자산 ID 목록의 전체 스케줄 라인을 한 번에 조회 (asset JOIN FETCH로 lazy load 방지) */
  @Query("select l from DepreciationScheduleLine l join fetch l.asset where l.asset.id in :assetIds")
  List<DepreciationScheduleLine> findAllByAssetIdIn(@Param("assetIds") List<Long> assetIds);

  List<DepreciationScheduleLine> findByAsset_IdAndVersionNoOrderByPeriodNoAsc(Long assetId, Integer versionNo);

  @Query("""
    select l from DepreciationScheduleLine l
    where l.asset.id = :assetId
      and l.versionNo = :versionNo
      and l.depreciationDate is not null
      and l.depreciationDate <= :asOf
    order by l.depreciationDate desc, l.periodNo desc
  """)
  List<DepreciationScheduleLine> findLatestLineUpTo(Long assetId, Integer versionNo, LocalDate asOf);

  @Query("""
    select l
    from DepreciationScheduleLine l
    where l.asset.id = :assetId
      and l.versionNo = :versionNo
      and l.periodNo > 0
      and l.depreciationDate >= :startDate
      and l.depreciationDate <= :endDate
    order by l.periodNo asc
  """)
  List<DepreciationScheduleLine> findLinesInMonth(Long assetId, Integer versionNo, LocalDate startDate, LocalDate endDate);

  @Query("""
    select coalesce(max(l.periodNo), 0)
    from DepreciationScheduleLine l
    where l.asset.id = :assetId
      and l.versionNo = :versionNo
  """)
  int findMaxPeriodNo(Long assetId, Integer versionNo);
}