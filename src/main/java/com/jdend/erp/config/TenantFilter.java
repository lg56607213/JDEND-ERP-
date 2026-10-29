package com.jdend.erp.config;

import com.jdend.erp.auth.entity.LoginUser;
import com.jdend.erp.auth.repository.LoginUserRepository;
import com.jdend.erp.auth.service.AuthService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantFilter implements Filter {

    // 서버 간 공개 API (홈페이지 Worker 등). 세션 대신 Authorization: Bearer 키로 인증하고,
    // 키에 묶인 회사(login_users.login_id)의 DB로 라우팅한다.
    private static final String SERVER_PUBLIC_API_PREFIX = "/api/public/";

    // 키로 찾은 회사 DB 캐시 (클라이언트 이름 → targetDb, 확인 시각). 회사 비활성화가 늦어도 5분 안에 반영되게 짧게 둔다.
    private static final long TENANT_CACHE_TTL_MS = 5 * 60 * 1000L;

    private final PublicApiProperties publicApiProperties;
    // 필터는 서블릿 기동 시점에 먼저 만들어지므로 JPA/DB 빈은 실제 요청 때 꺼내 쓴다(조기 초기화 방지)
    private final ObjectProvider<LoginUserRepository> loginUserRepository;
    private final ObjectProvider<TenantDatabaseService> tenantDatabaseService;

    private final Map<String, CachedTenant> publicTenantCache = new ConcurrentHashMap<>();

    private record CachedTenant(String targetDb, long checkedAt) {}

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
            if (uri.startsWith(SERVER_PUBLIC_API_PREFIX)) {
                // 서버 간 공개 API: 세션을 보지 않는다. 키 확인 → 회사 DB 결정
                PublicApiProperties.Client client = matchPublicClient(req.getHeader("Authorization"));
                if (client == null) {
                    writeJson(res, HttpServletResponse.SC_UNAUTHORIZED, "인증 키가 올바르지 않습니다.");
                    return;
                }
                String targetDb = resolvePublicTenantDb(client);
                if (targetDb == null) {
                    writeJson(res, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "연동 회사 설정을 확인할 수 없습니다.");
                    return;
                }
                TenantContext.setCurrentDb(targetDb);
            } else if (uri.startsWith("/api/auth") || uri.startsWith("/api/tax-consultations")) {
                TenantContext.setCurrentDb("auth");
            } else if (uri.startsWith("/api/loan-applications")) {
                // 증차 자금 신청 — 신청 데이터는 auth DB에 있지만, 보유 차량 수는
                // 업체 DB에서 읽으므로 세션의 회사 DB로 라우팅하고 서비스가 필요할 때
                // auth로 전환한다.
                HttpSession s3 = req.getSession(false);
                String db3 = s3 == null ? null : (String) s3.getAttribute(AuthService.SESSION_TARGET_DB);
                TenantContext.setCurrentDb(db3 == null || db3.isBlank() ? "auth" : db3);
                if (s3 == null || s3.getAttribute(AuthService.SESSION_LOGIN_ID) == null) {
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"message\":\"로그인이 필요합니다.\"}");
                    return;
                }
            } else if (uri.startsWith("/api/contract-sign")) {
                // 계약서 전자서명: 고객이 로그인 없이 여는 공개 경로.
                // 서명 요청 토큰은 auth DB에 있고, 회사 DB 전환은 서비스가 직접 한다.
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

    /** Bearer 키를 설정된 클라이언트 키와 상수 시간 비교. 빈 키는 어떤 경우에도 통과시키지 않는다. */
    private PublicApiProperties.Client matchPublicClient(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) return null;
        String presented = authorization.substring(7).trim();
        if (presented.isEmpty()) return null;

        byte[] presentedHash = sha256(presented);
        PublicApiProperties.Client matched = null;
        for (PublicApiProperties.Client c : publicApiProperties.getClients()) {
            String key = c.getKey();
            if (key == null || key.isBlank()) continue;  // 키 미설정 = 비활성
            // 길이가 다른 키도 같은 시간이 걸리도록 해시끼리 비교
            if (MessageDigest.isEqual(presentedHash, sha256(key.trim())) && matched == null) {
                matched = c;
            }
        }
        return matched;
    }

    /** 클라이언트에 묶인 회사의 DB 이름을 찾고 데이터소스를 등록한다. 실패하면 null */
    private String resolvePublicTenantDb(PublicApiProperties.Client client) {
        String cacheKey = client.getName() + "|" + client.getTenantLoginId();
        CachedTenant cached = publicTenantCache.get(cacheKey);
        long now = System.currentTimeMillis();
        if (cached != null && now - cached.checkedAt() < TENANT_CACHE_TTL_MS) {
            return cached.targetDb();
        }

        String loginId = client.getTenantLoginId();
        if (loginId == null || loginId.isBlank()) {
            log.warn("[PublicApi] 클라이언트 {} 의 tenant-login-id 가 비어 있습니다.", client.getName());
            return null;
        }

        // login_users 는 auth DB 에만 있다
        String saved = TenantContext.getCurrentDb();
        Optional<LoginUser> user;
        TenantContext.setCurrentDb("auth");
        try {
            user = loginUserRepository.getObject().findByLoginId(loginId.trim());
        } finally {
            TenantContext.setCurrentDb(saved);
        }

        if (user.isEmpty() || !Boolean.TRUE.equals(user.get().getIsActive())) {
            log.warn("[PublicApi] 클라이언트 {}: 회사 {} 이(가) 없거나 비활성입니다.", client.getName(), loginId);
            publicTenantCache.remove(cacheKey);
            return null;
        }
        String targetDb = user.get().getTargetDb();
        try {
            tenantDatabaseService.getObject().registerExistingTenantDataSource(targetDb);
        } catch (RuntimeException e) {
            log.warn("[PublicApi] 클라이언트 {}: 회사 DB {} 등록 실패 - {}", client.getName(), targetDb, e.getMessage());
            return null;
        }
        publicTenantCache.put(cacheKey, new CachedTenant(targetDb, now));
        return targetDb;
    }

    private static byte[] sha256(String s) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void writeJson(HttpServletResponse res, int status, String message) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}