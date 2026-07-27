package com.jdend.erp.accounting.prepaidrent.repository;

import com.jdend.erp.accounting.prepaidrent.entity.PrepaidRent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PrepaidRentRepository extends JpaRepository<PrepaidRent, Long> {

    /** 특정 계약의 거래 이력 (날짜·ID 오름차순) */
    List<PrepaidRent> findByContractIdOrderByTransactionDateAscIdAsc(Long contractId);

    /**
     * 여러 계약 ID에 대한 (contractId, transactionType) 별 합계.
     * Object[]: [contractId(Long), transactionType(String), sum(Long)]
     */
    @Query("""
        select p.contractId, p.transactionType, sum(p.amount)
        from PrepaidRent p
        where p.contractId in :contractIds
        group by p.contractId, p.transactionType
    """)
    List<Object[]> sumByContractIds(@Param("contractIds") List<Long> contractIds);

    /**
     * 여러 계약 ID에 대한 최근 입금일.
     * Object[]: [contractId(Long), maxDate(LocalDate)]
     */
    @Query("""
        select p.contractId, max(p.transactionDate)
        from PrepaidRent p
        where p.contractId in :contractIds and p.transactionType = '입금'
        group by p.contractId
    """)
    List<Object[]> lastDepositDateByContractIds(@Param("contractIds") List<Long> contractIds);

    /** prepaid_rents 테이블에 거래가 있는 고유 계약 ID 목록 */
    @Query("select distinct p.contractId from PrepaidRent p")
    List<Long> findDistinctContractIds();
}
