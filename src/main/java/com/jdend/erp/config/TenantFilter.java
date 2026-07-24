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

    // 구독 결제 공개 엔드포인트 (jdend.co.kr 호출, 키움페이 서버 통지)
    private static final Set<String> SUBSCRIPTION_PUBLIC_PREFIXES = Set.of(
            "/api/subscription/kiwoom/"
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
            if (uri.startsWith("/api/auth") || uri.startsWith("/api/tax-consultations")) {
                TenantContext.setCurrentDb("auth");
            } else if (uri.startsWith("/api/company-users")) {
                // BUG-12-03: /api/company-users는 auth DB지만 인증 필수
                TenantContext.setCurrentDb("auth");
                HttpSession session = req.getSession(false);
                if (session == null || session.getAttribute(AuthService.SESSION_LOGIN_ID) == null) {
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"message\":\"로그인이 필요합니다.\"}");
                    return;
                }
            } else if (uri.startsWith("/api/subscription")) {
                // 구독 결제: 항상 auth DB 사용 (멀티테넌시 공유 DB)
                TenantContext.setCurrentDb("auth");
                // 공개 엔드포인트(/kiwoom/**)는 세션 불필요, 나머지는 ADMIN 세션 필요
                boolean isPublicSubscription = SUBSCRIPTION_PUBLIC_PREFIXES.stream()
                        .anyMatch(uri::startsWith);
                if (!isPublicSubscription) {
                    HttpSession session = req.getSession(false);
                    if (session == null || session.getAttribute(AuthService.SESSION_LOGIN_ID) == null) {
                        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        res.setContentType("application/json;charset=UTF-8");
                        res.getWriter().write("{\"message\":\"로그인이 필요합니다.\"}");
                        return;
                    }
                }
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