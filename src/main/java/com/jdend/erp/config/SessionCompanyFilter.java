package com.jdend.erp.config;

import com.jdend.erp.auth.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// @Component 제거 — DbContextHolder는 DB 라우팅에 사용되지 않음. TenantFilter/TenantContext가 역할 담당.
public class SessionCompanyFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {

    try {
      HttpSession session = request.getSession(false);
      if (session != null) {
        Object targetDb = session.getAttribute(AuthService.SESSION_TARGET_DB);
        if (targetDb != null) {
          DbContextHolder.set(String.valueOf(targetDb));
        }
      }
      filterChain.doFilter(request, response);
    } finally {
      DbContextHolder.clear();
    }
  }
}