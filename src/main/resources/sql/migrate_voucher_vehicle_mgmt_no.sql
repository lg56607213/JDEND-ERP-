-- vouchers 테이블에 차량관리번호 컬럼 추가 (S2)
-- 기존 전표는 null (소급 불필요), 이후 생성분부터 채워짐
-- 각 회사 DB에 실행 (erp_company_a, erp_company_b 등)
-- MySQL 8.0 호환: 컬럼 없을 때만 추가 (IF NOT EXISTS 미지원 대체)
SET @exist := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name   = 'vouchers'
    AND column_name  = 'vehicle_mgmt_no'
);
SET @sql := IF(@exist = 0,
  'ALTER TABLE vouchers ADD COLUMN vehicle_mgmt_no VARCHAR(30) NULL',
  'SELECT ''vehicle_mgmt_no already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 인덱스 없을 때만 생성
SET @idxExist := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'vouchers'
    AND index_name = 'idx_voucher_vehicle_mgmt_no'
);
SET @idxSql := IF(@idxExist = 0,
  'CREATE INDEX idx_voucher_vehicle_mgmt_no ON vouchers(vehicle_mgmt_no)',
  'SELECT ''index already exists''');
PREPARE stmt2 FROM @idxSql; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;
