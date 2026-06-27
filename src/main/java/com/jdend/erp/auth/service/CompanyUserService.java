package com.jdend.erp.auth.service;

import com.jdend.erp.auth.dto.CompanyUserRequest;
import com.jdend.erp.auth.dto.CompanyUserResponse;
import com.jdend.erp.auth.entity.CompanyUser;
import com.jdend.erp.auth.repository.CompanyUserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/** 회사 자체의 사용자(직원 로그인 계정) 관리 — 항상 세션에 들어있는 자기 회사(companyId) 범위로만 동작한다. */
@Service
@RequiredArgsConstructor
public class CompanyUserService {

  private final CompanyUserRepository companyUserRepository;
  private final PermissionService permissionService;

  @Transactional(readOnly = true)
  public List<CompanyUserResponse> list(HttpSession session) {
    permissionService.requireCompanyAdmin(session);
    Long companyId = requireCompanyId(session);

    return companyUserRepository.findByCompanyIdOrderByIdAsc(companyId).stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional
  public CompanyUserResponse create(CompanyUserRequest req, HttpSession session) {
    permissionService.requireCompanyAdmin(session);
    Long companyId = requireCompanyId(session);

    if (req == null) throw new RuntimeException("요청값이 없습니다.");
    if (isBlank(req.getUserLoginId())) throw new RuntimeException("사용자 아이디를 입력해주세요.");
    if (isBlank(req.getUserPassword())) throw new RuntimeException("사용자 비밀번호를 입력해주세요.");

    String userLoginId = req.getUserLoginId().trim();

    if (companyUserRepository.existsByCompanyIdAndUserLoginId(companyId, userLoginId)) {
      throw new RuntimeException("이미 존재하는 사용자 아이디입니다.");
    }

    CompanyUser saved = companyUserRepository.save(
        CompanyUser.builder()
            .companyId(companyId)
            .userLoginId(userLoginId)
            .userPassword(req.getUserPassword().trim())
            .role(normalizeRole(req.getRole()))
            .isActive(req.getIsActive() == null ? true : req.getIsActive())
            .build()
    );

    return toResponse(saved);
  }

  @Transactional
  public CompanyUserResponse update(Long id, CompanyUserRequest req, HttpSession session) {
    permissionService.requireCompanyAdmin(session);
    Long companyId = requireCompanyId(session);

    CompanyUser user = findOwnedUser(id, companyId);

    if (req == null) throw new RuntimeException("요청값이 없습니다.");

    if (!isBlank(req.getUserPassword())) {
      user.setUserPassword(req.getUserPassword().trim());
    }
    if (!isBlank(req.getRole())) {
      user.setRole(normalizeRole(req.getRole()));
    }
    if (req.getIsActive() != null) {
      user.setIsActive(req.getIsActive());
    }

    return toResponse(user);
  }

  @Transactional
  public void delete(Long id, HttpSession session) {
    permissionService.requireCompanyAdmin(session);
    Long companyId = requireCompanyId(session);

    CompanyUser user = findOwnedUser(id, companyId);

    String currentLoginId = (String) session.getAttribute(AuthService.SESSION_LOGIN_ID);
    if (Objects.equals(currentLoginId, user.getUserLoginId())) {
      throw new RuntimeException("현재 로그인한 계정은 삭제할 수 없습니다.");
    }

    companyUserRepository.delete(user);
  }

  private CompanyUser findOwnedUser(Long id, Long companyId) {
    CompanyUser user = companyUserRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다. id=" + id));

    if (!Objects.equals(user.getCompanyId(), companyId)) {
      throw new RuntimeException("다른 회사의 사용자입니다.");
    }

    return user;
  }

  private Long requireCompanyId(HttpSession session) {
    Object companyId = session.getAttribute(AuthService.SESSION_COMPANY_ID);
    if (companyId == null) {
      throw new RuntimeException("회사관리자 계정으로 로그인해야 사용자관리를 이용할 수 있습니다.");
    }
    return (Long) companyId;
  }

  private CompanyUserResponse toResponse(CompanyUser u) {
    return CompanyUserResponse.builder()
        .id(u.getId())
        .userLoginId(u.getUserLoginId())
        .role(u.getRole())
        .isActive(u.getIsActive())
        .build();
  }

  private String normalizeRole(String role) {
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
