package com.jdend.erp.config;

import com.jdend.erp.auth.service.AuthService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class TenantFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;

        try {
            String uri = req.getRequestURI();

            // login_users/company_users는 운영 DB(auth)에만 있고 회사별 DB로는 복제되지 않으므로,
            // 이 두 테이블을 쓰는 API는 세션의 회사 DB와 무관하게 항상 auth로 라우팅한다.
            if (uri.startsWith("/api/auth") || uri.startsWith("/api/company-users")) {
                TenantContext.setCurrentDb("auth");
            } else {
                HttpSession session = req.getSession(false);

                if (session == null) {
                    TenantContext.setCurrentDb("auth");
                } else {
                    String targetDb = (String) session.getAttribute(AuthService.SESSION_TARGET_DB);
                    TenantContext.setCurrentDb(
                            targetDb == null || targetDb.isBlank() ? "auth" : targetDb
                    );
                }
            }

            chain.doFilter(request, response);

        } finally {
            TenantContext.clear();
        }
    }
}