package com.jdend.erp.loan.repository;

import com.jdend.erp.loan.entity.LoanApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {

    @Query("""
      select a from LoanApplication a
      where a.companyCode = :companyCode
        and (:status = '' or a.status = :status)
        and (:from is null or a.createdAt >= :from)
        and (:to   is null or a.createdAt <= :to)
      order by a.id desc
    """)
    List<LoanApplication> search(@Param("companyCode") String companyCode,
                                 @Param("status") String status,
                                 @Param("from") LocalDateTime from,
                                 @Param("to") LocalDateTime to);

    long countByCompanyCodeAndStatus(String companyCode, String status);
}
