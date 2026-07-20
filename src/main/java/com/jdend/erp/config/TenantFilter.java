package com.jdend.erp.config;

import com.jdend.erp.auth.service.AuthService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

@Component
public class TenantFilter implements Filter {

    // 로그인 없이 접근 가능한 API 경로
    private static final Set<String> PUBLIC_API_PREFIXES = Set.of(
            "/api/auth",
            "/api/company-applications"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        try {
            String uri = req.getRequestURI();

            // login_users/company_users는 운영 DB(auth)에만 있고 회사별 DB로는 복제되지 않으므로,
            // 이 두 테이블을 쓰는 API는 세션의 회사 DB와 무관하게 항상 auth로 라우팅한다.
            if (uri.startsWith("/api/auth") || uri.startsWith("/api/company-users") || uri.startsWith("/api/tax-consultations")) {
                TenantContext.setCurrentDb("auth");
            } else if (uri.startsWith("/api/")) {
                HttpSession session = req.getSession(false);
                boolean isPublic = PUBLIC_API_PREFIXES.stream().anyMatch(uri::startsWith);

                if (session == null && !isPublic) {
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"message\":\"로그인이 필요합니다.\"}");
                    return;
                }

                String targetDb = session == null ? null
                        : (String) session.getAttribute(AuthService.SESSION_TARGET_DB);
                TenantContext.setCurrentDb(
                        targetDb == null || targetDb.isBlank() ? "auth" : targetDb
                );
            }

            chain.doFilter(request, response);

        } finally {
            TenantContext.clear();
        }
    }
}