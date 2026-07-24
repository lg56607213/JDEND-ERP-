package com.jdend.erp.subscription.repository;

import com.jdend.erp.subscription.entity.SubscriptionPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionPaymentRepository extends JpaRepository<SubscriptionPayment, Long> {

    Optional<SubscriptionPayment> findByOrderId(String orderId);

    Optional<SubscriptionPayment> findByDaoutrx(String daoutrx);

    boolean existsByDaoutrx(String daoutrx);

    List<SubscriptionPayment> findAllByOrderByCreatedAtDesc();

    List<SubscriptionPayment> findByStatusOrderByCreatedAtDesc(String status);
}
