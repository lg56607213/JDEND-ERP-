package com.jdend.erp.myinfo.repository;

import com.jdend.erp.myinfo.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {
    List<BankAccount> findAllByOrderByIdAsc();
    List<BankAccount> findByIsActiveTrueOrderByIdAsc();
}
