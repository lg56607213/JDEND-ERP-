package com.jdend.erp.accounting.depreciation.repository;

import com.jdend.erp.accounting.depreciation.entity.DepreciationScheduleLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface DepreciationScheduleLineRepository extends JpaRepository<DepreciationScheduleLine, Long> {

  @Query("select coalesce(max(l.versionNo), 0) from DepreciationScheduleLine l where l.asset.id = :assetId")
  int findMaxVersion(Long assetId);

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