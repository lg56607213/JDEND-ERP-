package com.jdend.erp.accounting.voucher.repository;

import com.jdend.erp.accounting.voucher.entity.VoucherSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface VoucherSequenceRepository extends JpaRepository<VoucherSequence, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from VoucherSequence s where s.datePrefix = :prefix")
    Optional<VoucherSequence> findByDatePrefixForUpdate(@Param("prefix") String prefix);
}
