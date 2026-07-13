-- vouchers 테이블에 차량관리번호 컬럼 추가 (S2)
-- 기존 전표는 null (소급 불필요), 이후 생성분부터 채워짐
-- 각 회사 DB에 실행 (erp_company_a, erp_company_b 등)
ALTER TABLE vouchers ADD COLUMN IF NOT EXISTS vehicle_mgmt_no VARCHAR(30) NULL;
CREATE INDEX IF NOT EXISTS idx_voucher_vehicle_mgmt_no ON vouchers(vehicle_mgmt_no);
