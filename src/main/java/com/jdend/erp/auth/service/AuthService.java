package com.jdend.erp.auth.service;

import com.jdend.erp.auth.dto.*;
import com.jdend.erp.auth.entity.CompanyUser;
import com.jdend.erp.auth.entity.LoginUser;
import com.jdend.erp.auth.repository.CompanyUserRepository;
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
  public static final String SESSION_COMPANY_ID = "COMPANY_ID";

  private final LoginUserRepository loginUserRepository;
  private final CompanyUserRepository companyUserRepository;
  private final TenantDatabaseService tenantDatabaseService;

  // 2단계 로그인: ① 통합 아이디/비밀번호로 회사(또는 운영자) 확인 ② 회사면 사용자 아이디/비밀번호까지 확인.
  @Transactional
  public LoginResponse login(LoginRequest req, HttpSession session) {
    if (req == null) throw new IllegalArgumentException("로그인 요청값이 없습니다.");
    if (isBlank(req.getCompanyLoginId())) throw new IllegalArgumentException("통합 아이디를 입력해주세요.");
    if (isBlank(req.getCompanyPassword())) throw new IllegalArgumentException("통합 비밀번호를 입력해주세요.");

    LoginUser company = loginUserRepository.findByLoginId(req.getCompanyLoginId().trim())
        .orElseThrow(() -> new IllegalArgumentException("통합 아이디 또는 비밀번호가 올바르지 않습니다."));

    if (!Boolean.TRUE.equals(company.getIsActive())) {
      throw new IllegalArgumentException("비활성화된 계정입니다.");
    }
    if (!company.getLoginPassword().equals(req.getCompanyPassword().trim())) {
      throw new IllegalArgumentException("통합 아이디 또는 비밀번호가 올바르지 않습니다.");
    }

    if ("PLATFORM_ADMIN".equals(company.getRole())) {
      session.setAttribute(SESSION_LOGIN_ID, company.getLoginId());
      session.setAttribute(SESSION_COMPANY_NAME, company.getCompanyName());
      session.setAttribute(SESSION_TARGET_DB, "auth");
      session.setAttribute(SESSION_ROLE, "ADMIN");
      session.removeAttribute(SESSION_COMPANY_ID);

      return LoginResponse.builder()
          .success(true)
          .loginId(company.getLoginId())
          .companyName(company.getCompanyName())
          .role("ADMIN")
          .message("로그인 성공")
          .build();
    }

    CompanyUser user;

    if (isBlank(req.getUserLoginId()) && isBlank(req.getUserPassword())) {
      // 사용자 아이디/비밀번호를 비워두면 통합 계정과 같은 아이디/비밀번호로 자동 생성된
      // 그 회사의 첫 사용자(회사관리자)로 바로 로그인한다(운영자 로그인과 동일한 편의).
      user = companyUserRepository.findByCompanyIdAndUserLoginId(company.getId(), company.getLoginId())
          .filter(u -> u.getUserPassword().equals(company.getLoginPassword()))
          .orElseThrow(() -> new IllegalArgumentException("사용자 아이디를 입력해주세요."));
    } else {
      if (isBlank(req.getUserLoginId())) throw new IllegalArgumentException("사용자 아이디를 입력해주세요.");
      if (isBlank(req.getUserPassword())) throw new IllegalArgumentException("사용자 비밀번호를 입력해주세요.");

      user = companyUserRepository.findByCompanyIdAndUserLoginId(company.getId(), req.getUserLoginId().trim())
          .orElseThrow(() -> new IllegalArgumentException("사용자 아이디 또는 비밀번호가 올바르지 않습니다."));

      if (!user.getUserPassword().equals(req.getUserPassword().trim())) {
        throw new IllegalArgumentException("사용자 아이디 또는 비밀번호가 올바르지 않습니다.");
      }
    }

    if (!Boolean.TRUE.equals(user.getIsActive())) {
      throw new IllegalArgumentException("비활성화된 사용자입니다.");
    }

    String role = normalizeUserRole(user.getRole());
    String targetDb = company.getTargetDb();
    tenantDatabaseService.ensureTenantDatabase(targetDb);

    session.setAttribute(SESSION_LOGIN_ID, user.getUserLoginId());
    session.setAttribute(SESSION_COMPANY_NAME, company.getCompanyName());
    session.setAttribute(SESSION_TARGET_DB, targetDb);
    session.setAttribute(SESSION_ROLE, role);
    session.setAttribute(SESSION_COMPANY_ID, company.getId());

    return LoginResponse.builder()
        .success(true)
        .loginId(user.getUserLoginId())
        .companyName(company.getCompanyName())
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

  // ==========================
  // 운영자 전용: 회사(통합 계정) CRUD
  // ==========================
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
    if (isBlank(req.getLoginId())) throw new RuntimeException("통합 아이디를 입력해주세요.");
    if (isBlank(req.getLoginPassword())) throw new RuntimeException("통합 비밀번호를 입력해주세요.");
    if (isBlank(req.getCompanyName())) throw new RuntimeException("회사명을 입력해주세요.");

    String loginId = req.getLoginId().trim();
    String loginPassword = req.getLoginPassword().trim();

    if (loginUserRepository.existsByLoginId(loginId)) {
      throw new RuntimeException("이미 존재하는 아이디입니다.");
    }

    String companyName = req.getCompanyName().trim();
    String targetDb = tenantDatabaseService.normalizeTenantDb(req.getTargetDb(), loginId);

    tenantDatabaseService.ensureTenantDatabase(targetDb);

    LoginUser saved = loginUserRepository.save(
        LoginUser.builder()
            .loginId(loginId)
            .loginPassword(loginPassword)
            .companyName(companyName)
            .targetDb(targetDb)
            .role("COMPANY")
            .isActive(req.getIsActive() == null ? true : req.getIsActive())
            .build()
    );

    // 통합 계정과 같은 아이디/비밀번호로 그 회사의 첫 사용자(회사관리자)를 자동 생성해
    // 운영자가 추가 작업 없이 바로 첫 로그인을 회사에 넘겨줄 수 있게 한다.
    companyUserRepository.save(
        CompanyUser.builder()
            .companyId(saved.getId())
            .userLoginId(loginId)
            .userPassword(loginPassword)
            .role("COMPANY_ADMIN")
            .isActive(true)
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

    companyUserRepository.findByCompanyIdOrderByIdAsc(user.getId()).forEach(companyUserRepository::delete);
    loginUserRepository.delete(user);
  }

  public void requireAdmin(HttpSession session) {
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
        .role(u.getRole())
        .isActive(u.getIsActive())
        .build();
  }

  // 회사 내부 사용자 역할: COMPANY_ADMIN(회사관리자) / MANAGER(책임자) / STAFF(실무자).
  // 인식 못하는 값은 안전한 기본값인 STAFF로 취급한다.
  private String normalizeUserRole(String role) {
    if (role == null || role.isBlank()) return "STAFF";

    String r = role.trim().toUpperCase();

    if ("COMPANY_ADMIN".equals(r)) return "COMPANY_ADMIN";
    if ("MANAGER".equals(r)) return "MANAGER";

    return "STAFF";
  }

  private boolean isBlank(String s) {
    return s == null || s.trim().isEmpty();
  }
}
