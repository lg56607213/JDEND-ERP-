package com.jdend.erp.config;

import com.jdend.erp.auth.entity.CompanyUser;
import com.jdend.erp.auth.repository.CompanyUserRepository;
import com.jdend.erp.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

/**
 * BUG-12-01: 사용자 계정 삭제(비활성화) 후 기존 세션 자동 무효화.
 * 모든 /api/** 요청 진입 시, 세션의 COMPANY_USER_ID로 DB를 조회해
 * isActive=false이거나 레코드가 없으면 세션을 즉시 무효화하고 401을 반환한다.
 *
 * company_users는 auth DB에 있으므로, TenantFilter가 이미 DB 컨텍스트를 설정한 뒤에
 * 인터셉터가 실행된다. auth 외의 경로에서도 안전하게 조회할 수 있도록
 * 쿼리 실행 전 TenantContext를 "auth"로 임시 전환 후 복원한다.
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final CompanyUserRepository companyUserRepository;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        HttpSession session = request.getSession(false);
        if (session == null) return true;  // 세션 없음 → TenantFilter가 이미 처리

        Object companyUserIdObj = session.getAttribute(AuthService.SESSION_COMPANY_USER_ID);
        if (companyUserIdObj == null) return true;  // ADMIN / TAX_AGENT — 회사 사용자 없음

        Long companyUserId = (Long) companyUserIdObj;

        // auth DB로 임시 전환해 company_users 조회 (현재 DB 컨텍스트 복원 보장)
        String savedDb = TenantContext.getCurrentDb();
        TenantContext.setCurrentDb("auth");
        Optional<CompanyUser> userOpt;
        try {
            userOpt = companyUserRepository.findById(companyUserId);
        } finally {
            TenantContext.setCurrentDb(savedDb);
        }

        boolean inactive = userOpt.isEmpty() || !Boolean.TRUE.equals(userOpt.get().getIsActive());
        if (inactive) {
            session.invalidate();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"계정이 비활성화되었거나 삭제되었습니다. 다시 로그인해주세요.\"}");
            return false;
        }

        return true;
    }
}
