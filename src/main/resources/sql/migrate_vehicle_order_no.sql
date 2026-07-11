-- ============================================================================
-- Phase 2 - S1: 차량 번호체계(발주번호) 컬럼 추가 마이그레이션
-- ============================================================================
-- 변경 요청: 1발주=N대 지원 + 발주번호(J+YYMMDD+발주순번3) 도입.
--   VehicleOrder 엔티티에 order_no(발주번호 10자리) 컬럼 추가.
--   개별 식별(차량관리번호 13자리, vehicle_mgmt_no)은 발주 시점에 확정하며 기존과 동일하게 UNIQUE.
--   → 여러 대(N행)가 같은 order_no(발주번호)를 그룹 헤더로 공유한다.
--
-- 적용 대상 DB (vehicle_orders 테이블이 있는 모든 DB):
--   - 템플릿 DB: erp   (⇐ 신규 회사 DB가 CREATE TABLE ... LIKE 로 이 스키마를 상속하므로 반드시 반영)
--   - 기존 회사 DB: erp_company_*   (각 DB마다 실행)
--
-- ddl-auto=none 환경 기준. 로컬에서 ddl-auto:update 로 띄우면 엔티티 기준 자동 반영될 수도 있으나,
-- 운영/배포는 이 스크립트로 명시적으로 반영한다.
--
-- ★ 테스트 단계 권장(실데이터 없음): 기존 회사 테스트 DB(erp_company_*)는 아래 ALTER 대신
--   drop 후 재생성이 가장 깔끔하다. 재생성 시 템플릿 DB(erp)에 order_no가 반영돼 있어야
--   새 회사 DB가 자동 상속한다. (템플릿 erp 에는 반드시 ALTER 실행)
-- ============================================================================

-- 1) 템플릿 DB(erp)에 반영 --------------------------------------------------
ALTER TABLE `erp`.`vehicle_orders`
  ADD COLUMN `order_no` VARCHAR(10) NULL AFTER `vehicle_mgmt_no`;

-- (선택) 그룹 조회 성능용 인덱스
-- ALTER TABLE `erp`.`vehicle_orders` ADD INDEX `idx_vehicle_orders_order_no` (`order_no`);

-- 2) 기존 회사 DB에 반영 -----------------------------------------------------
--   회사 DB마다 아래를 실행(예시). 테스트 DB는 drop 후 재생성 권장.
-- ALTER TABLE `erp_company_test111`.`vehicle_orders`
--   ADD COLUMN `order_no` VARCHAR(10) NULL AFTER `vehicle_mgmt_no`;
-- ALTER TABLE `erp_company_test112`.`vehicle_orders`
--   ADD COLUMN `order_no` VARCHAR(10) NULL AFTER `vehicle_mgmt_no`;
-- ALTER TABLE `erp_company_test113`.`vehicle_orders`
--   ADD COLUMN `order_no` VARCHAR(10) NULL AFTER `vehicle_mgmt_no`;

-- 참고: 기존 J0001 형식 데이터는 order_no 가 NULL 로 남는다(신규분부터 신규 포맷 적용, 마이그레이션 안 함).
