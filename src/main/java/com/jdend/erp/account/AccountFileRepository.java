package com.jdend.erp.account;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AccountFileRepository extends JpaRepository<AccountFile, Long> {
    List<AccountFile> findByAccountId(Long accountId);
}
