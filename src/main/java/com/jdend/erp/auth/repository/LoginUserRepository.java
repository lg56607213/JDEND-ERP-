package com.jdend.erp.auth.repository;

import com.jdend.erp.auth.entity.LoginUser;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface LoginUserRepository extends JpaRepository<LoginUser, Long> {

  Optional<LoginUser> findByLoginId(String loginId);

  boolean existsByLoginId(String loginId);

  @Query("""
    select u
    from LoginUser u
    where (:kw = '' or
      lower(u.loginId) like concat('%', lower(:kw), '%') or
      lower(u.companyName) like concat('%', lower(:kw), '%') or
      lower(u.role) like concat('%', lower(:kw), '%'))
    order by u.id desc
  """)
  List<LoginUser> search(@Param("kw") String kw);

  @Query("select u.targetDb from LoginUser u where u.role = 'COMPANY' and u.isActive = true")
  List<String> findAllActiveCompanyTargetDbs();
}