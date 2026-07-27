package com.jdend.erp.accounting.deposit.repository;

import com.jdend.erp.accounting.deposit.entity.DepositTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DepositTransactionRepository extends JpaRepository<DepositTransaction, Long> {

    /** 특정 계약+유형의 거래 이력 (날짜·ID 오름차순) */
    List<DepositTransaction> findByContractIdAndDepositTypeOrderByTransactionDateAscIdAsc(
            Long contractId, String depositType);

    /**
     * 여러 계약 ID에 대한 (contractId, depositType, transactionType) 별 합계.
     * Object[]: [contractId(Long), depositType(String), transactionType(String), sum(Long)]
     */
    @Query("""
        select d.contractId, d.depositType, d.transactionType, sum(d.amount)
        from DepositTransaction d
        where d.contractId in :contractIds
        group by d.contractId, d.depositType, d.transactionType
    """)
    List<Object[]> sumByContractIds(@Param("contractIds") List<Long> contractIds);

    /**
     * 여러 계약 ID에 대한 (contractId, depositType) 별 최근 거래일.
     * Object[]: [contractId(Long), depositType(String), maxDate(LocalDate)]
     */
    @Query("""
        select d.contractId, d.depositType, max(d.transactionDate)
        from DepositTransaction d
        where d.contractId in :contractIds
        group by d.contractId, d.depositType
    """)
    List<Object[]> lastDateByContractIds(@Param("contractIds") List<Long> contractIds);
}
