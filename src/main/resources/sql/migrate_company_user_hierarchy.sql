-- 회사 통합계정 + 회사별 사용자관리 도입 마이그레이션
--
-- * ddl-auto=none이라 자동 실행 안 됨. 운영 DB(erp, 즉 auth 데이터소스)에만 실행하면 된다.
--   company_users/login_users는 회사별 DB(erp_company_*)로는 복제되지 않는 운영 전용 테이블이다.
-- * login_users는 그대로 두고 role 값의 의미만 정리한다:
--     PLATFORM_ADMIN = 운영자 자신(JDEND ERP 운영사, admin 1행)
--     COMPANY        = 일반 회사 통합 계정(나머지 전부)
-- * 회사 통합 계정마다 기존 로그인 정보(아이디/비밀번호)를 그대로 써서
--   그 회사의 첫 번째 사용자(역할: COMPANY_ADMIN)를 자동 생성한다.

-- ========== 1) 기존 role 값 정리 ==========
UPDATE login_users SET role = 'PLATFORM_ADMIN' WHERE role = 'ADMIN';
UPDATE login_users SET role = 'COMPANY' WHERE role <> 'PLATFORM_ADMIN';

-- ========== 2) 회사 내부 사용자 테이블 ==========
CREATE TABLE IF NOT EXISTS company_users (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  company_id BIGINT NOT NULL,
  user_login_id VARCHAR(50) NOT NULL,
  user_password VARCHAR(100) NOT NULL,
  role VARCHAR(30) NOT NULL,
  is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT uk_company_user UNIQUE (company_id, user_login_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========== 3) 기존 회사 통합 계정마다 첫 사용자(회사관리자) 자동 생성 ==========
INSERT INTO company_users (company_id, user_login_id, user_password, role, is_active)
SELECT id, login_id, login_password, 'COMPANY_ADMIN', 1
FROM login_users
WHERE role = 'COMPANY';
