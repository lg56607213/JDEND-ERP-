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

            if (uri.startsWith("/api/auth")) {
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