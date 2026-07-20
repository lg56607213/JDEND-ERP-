package com.jdend.erp.auth.service;

import com.jdend.erp.auth.exception.ForbiddenException;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

/**
 * 등록 외(수정/삭제/승인) 동작에 책임자(MANAGER) 이상 권한을 요구하는 공통 체크.
 * 회사 내부에서 실무자(STAFF)는 등록만 가능하고, 책임자(MANAGER)/운영자(ADMIN)만
 * 기존 데이터를 고치거나 지우거나 전표를 승인할 수 있다.
 */
@Service
public class PermissionService {

  public void requireManager(HttpSession session) {
    String role = session == null ? null : (String) session.getAttribute(AuthService.SESSION_ROLE);

    if (!"ADMIN".equals(role) && !"COMPANY_ADMIN".equals(role) && !"MANAGER".equals(role)) {
      throw new ForbiddenException("책임자 권한이 필요합니다. 등록만 가능한 계정입니다.");
    }
  }

  // 회사 자체의 사용자관리(사용자 등록/수정/삭제) 화면 보호용 — 운영자 또는 그 회사의 회사관리자만 통과.
  public void requireCompanyAdmin(HttpSession session) {
    String role = session == null ? null : (String) session.getAttribute(AuthService.SESSION_ROLE);

    if (!"ADMIN".equals(role) && !"COMPANY_ADMIN".equals(role)) {
      throw new ForbiddenException("회사관리자 권한이 필요합니다.");
    }
  }

  // MT(정비) 기능 접근 보호 — 운영자이거나, 회사에 maintenanceEnabled 권한이 부여된 경우만 통과.
  public void requireMaintenance(HttpSession session) {
    String role = session == null ? null : (String) session.getAttribute(AuthService.SESSION_ROLE);
    if ("ADMIN".equals(role)) return;

    Boolean enabled = session == null ? null
        : (Boolean) session.getAttribute(AuthService.SESSION_MAINTENANCE_ENABLED);
    if (!Boolean.TRUE.equals(enabled)) {
      throw new ForbiddenException("MT(정비) 기능 접근 권한이 없습니다. 운영자에게 문의하세요.");
    }
  }
}
