package com.jdend.erp.loan.repository;

import com.jdend.erp.loan.entity.LoanApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {

    /** 신청 업체가 보는 자기 신청 내역 */
    List<LoanApplication> findByCompanyIdOrderByIdDesc(Long companyId);

    /** 운영자가 보는 전체 신청 */
    @Query("""
      select a from LoanApplication a
      where (:status = '' or a.status = :status)
        and (:from is null or a.createdAt >= :from)
        and (:to   is null or a.createdAt <= :to)
      order by a.id desc
    """)
    List<LoanApplication> searchAll(@Param("status") String status,
                                    @Param("from") LocalDateTime from,
                                    @Param("to") LocalDateTime to);

    long countByStatus(String status);
}
