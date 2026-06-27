package com.jdend.erp.auth.service;

import com.jdend.erp.auth.dto.*;
import com.jdend.erp.auth.entity.LoginUser;
import com.jdend.erp.auth.repository.LoginUserRepository;
import com.jdend.erp.config.TenantDatabaseService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthService {

  public static final String SESSION_LOGIN_ID = "LOGIN_ID";
  public static final String SESSION_COMPANY_NAME = "COMPANY_NAME";
  public static final String SESSION_TARGET_DB = "TARGET_DB";
  public static final String SESSION_ROLE = "ROLE";

  private final LoginUserRepository loginUserRepository;
  private final TenantDatabaseService tenantDatabaseService;

  @Transactional
  public LoginResponse login(LoginRequest req, HttpSession session) {
    if (req == null) throw new IllegalArgumentException("로그인 요청값이 없습니다.");
    if (isBlank(req.getLoginId())) throw new IllegalArgumentException("아이디를 입력해주세요.");
    if (isBlank(req.getLoginPassword())) throw new IllegalArgumentException("비밀번호를 입력해주세요.");

    LoginUser user = loginUserRepository.findByLoginId(req.getLoginId().trim())
        .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다."));

    if (!Boolean.TRUE.equals(user.getIsActive())) {
      throw new IllegalArgumentException("비활성화된 계정입니다.");
    }

    if (!user.getLoginPassword().equals(req.getLoginPassword().trim())) {
      throw new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다.");
    }

    String role = normalizeRole(user.getRole());
    String targetDb = user.getTargetDb();

    if (!"ADMIN".equals(role)) {
      tenantDatabaseService.ensureTenantDatabase(targetDb);
    }

    session.setAttribute(SESSION_LOGIN_ID, user.getLoginId());
    session.setAttribute(SESSION_COMPANY_NAME, user.getCompanyName());
    session.setAttribute(SESSION_TARGET_DB, targetDb);
    session.setAttribute(SESSION_ROLE, role);

    return LoginResponse.builder()
        .success(true)
        .loginId(user.getLoginId())
        .companyName(user.getCompanyName())
        .role(role)
        .message("로그인 성공")
        .build();
  }

  public LoginResponse me(HttpSession session) {
    String loginId = (String) session.getAttribute(SESSION_LOGIN_ID);
    String companyName = (String) session.getAttribute(SESSION_COMPANY_NAME);
    String role = (String) session.getAttribute(SESSION_ROLE);

    if (isBlank(loginId)) {
      return LoginResponse.builder()
          .success(false)
          .message("NOT_LOGGED_IN")
          .build();
    }

    return LoginResponse.builder()
        .success(true)
        .loginId(loginId)
        .companyName(companyName)
        .role(role)
        .message("로그인 상태")
        .build();
  }

  public void logout(HttpSession session) {
    session.invalidate();
  }

  @Transactional(readOnly = true)
  public List<LoginUserAdminResponse> adminList(String kw, HttpSession session) {
    requireAdmin(session);

    String q = kw == null ? "" : kw.trim();

    return loginUserRepository.search(q).stream()
        .map(this::toAdminResponse)
        .toList();
  }

  @Transactional
  public LoginUserAdminResponse adminCreate(LoginUserCreateRequest req, HttpSession session) {
    requireAdmin(session);

    if (req == null) throw new RuntimeException("요청값이 없습니다.");
    if (isBlank(req.getLoginId())) throw new RuntimeException("아이디를 입력해주세요.");
    if (isBlank(req.getLoginPassword())) throw new RuntimeException("비밀번호를 입력해주세요.");
    if (isBlank(req.getCompanyName())) throw new RuntimeException("회사명을 입력해주세요.");

    String loginId = req.getLoginId().trim();

    if (loginUserRepository.existsByLoginId(loginId)) {
      throw new RuntimeException("이미 존재하는 아이디입니다.");
    }

    String companyName = req.getCompanyName().trim();
    String targetDb = tenantDatabaseService.normalizeTenantDb(req.getTargetDb(), loginId);

    tenantDatabaseService.ensureTenantDatabase(targetDb);

    LoginUser saved = loginUserRepository.save(
        LoginUser.builder()
            .loginId(loginId)
            .loginPassword(req.getLoginPassword().trim())
            .companyName(companyName)
            .targetDb(targetDb)
            .role(normalizeRole(req.getRole()))
            .isActive(req.getIsActive() == null ? true : req.getIsActive())
            .build()
    );

    return toAdminResponse(saved);
  }

  @Transactional
  public LoginUserAdminResponse adminUpdate(Long id, LoginUserUpdateRequest req, HttpSession session) {
    requireAdmin(session);

    LoginUser user = loginUserRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("계정을 찾을 수 없습니다. id=" + id));

    if (req == null) throw new RuntimeException("요청값이 없습니다.");

    if (!isBlank(req.getLoginPassword())) {
      user.setLoginPassword(req.getLoginPassword().trim());
    }

    if (!isBlank(req.getCompanyName())) {
      user.setCompanyName(req.getCompanyName().trim());
    }

    if (!isBlank(req.getTargetDb())) {
      String targetDb = tenantDatabaseService.normalizeTenantDb(req.getTargetDb(), user.getLoginId());
      tenantDatabaseService.ensureTenantDatabase(targetDb);
      user.setTargetDb(targetDb);
    }

    if (!isBlank(req.getRole())) {
      user.setRole(normalizeRole(req.getRole()));
    }

    if (req.getIsActive() != null) {
      user.setIsActive(req.getIsActive());
    }

    return toAdminResponse(user);
  }

  @Transactional
  public void adminDelete(Long id, HttpSession session) {
    requireAdmin(session);

    LoginUser user = loginUserRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("계정을 찾을 수 없습니다. id=" + id));

    String currentLoginId = (String) session.getAttribute(SESSION_LOGIN_ID);

    if (Objects.equals(currentLoginId, user.getLoginId())) {
      throw new RuntimeException("현재 로그인한 운영자 계정은 삭제할 수 없습니다.");
    }

    loginUserRepository.delete(user);
  }

  private void requireAdmin(HttpSession session) {
    String role = (String) session.getAttribute(SESSION_ROLE);

    if (!"ADMIN".equals(role)) {
      throw new RuntimeException("운영자 권한이 필요합니다.");
    }
  }

  private LoginUserAdminResponse toAdminResponse(LoginUser u) {
    return LoginUserAdminResponse.builder()
        .id(u.getId())
        .loginId(u.getLoginId())
        .companyName(u.getCompanyName())
        .targetDb(u.getTargetDb())
        .role(normalizeRole(u.getRole()))
        .isActive(u.getIsActive())
        .build();
  }

  // ADMIN(운영사) / MANAGER(책임자: 등록+수정+삭제+승인) / STAFF(실무자: 등록만).
  // 인식 못하는 값(과거 "USER" 포함)은 안전한 기본값인 STAFF로 취급한다.
  private String normalizeRole(String role) {
    if (role == null || role.isBlank()) return "STAFF";

    String r = role.trim().toUpperCase();

    if ("ADMIN".equals(r)) return "ADMIN";
    if ("MANAGER".equals(r)) return "MANAGER";

    return "STAFF";
  }

  private boolean isBlank(String s) {
    return s == null || s.trim().isEmpty();
  }
}