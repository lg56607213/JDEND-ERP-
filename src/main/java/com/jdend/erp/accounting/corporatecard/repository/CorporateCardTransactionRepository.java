package com.jdend.erp.accounting.corporatecard.repository;

import com.jdend.erp.accounting.corporatecard.entity.CorporateCardTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface CorporateCardTransactionRepository extends JpaRepository<CorporateCardTransaction, Long> {

    /**
     * 조회기간 + 계정분류 필터.
     * accountCode 가 '__NONE__' 이면 계정분류가 아직 안 된 건만 조회한다.
     */
    @Query("""
        select c from CorporateCardTransaction c
        where (:startDate is null or c.useDate >= :startDate)
          and (:endDate is null or c.useDate <= :endDate)
          and (
                :accountCode is null or :accountCode = ''
                or (:accountCode = '__NONE__' and (c.accountCode is null or c.accountCode = ''))
                or c.accountCode = :accountCode
              )
        order by c.useDate asc, c.id asc
    """)
    List<CorporateCardTransaction> search(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("accountCode") String accountCode
    );

    /** 같은 날짜·금액·적요 조합이 이미 있으면 중복 업로드로 본다(적요 null 도 비교 대상). */
    @Query("""
        select count(c) from CorporateCardTransaction c
        where c.useDate = :useDate
          and c.amount = :amount
          and ((:summary is null and c.summary is null) or c.summary = :summary)
    """)
    long countDuplicates(
            @Param("useDate") LocalDate useDate,
            @Param("amount") Long amount,
            @Param("summary") String summary
    );
}
