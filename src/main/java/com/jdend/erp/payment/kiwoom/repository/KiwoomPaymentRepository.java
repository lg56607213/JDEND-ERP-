package com.jdend.erp.payment.kiwoom.repository;

import com.jdend.erp.payment.kiwoom.entity.KiwoomPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KiwoomPaymentRepository extends JpaRepository<KiwoomPayment, Long> {

    Optional<KiwoomPayment> findByOrderId(String orderId);

    Optional<KiwoomPayment> findByDaoutrx(String daoutrx);

    /** 중복 통지 방지용 — 동일 거래번호 이미 처리됐는지 확인 */
    boolean existsByDaoutrx(String daoutrx);
}
