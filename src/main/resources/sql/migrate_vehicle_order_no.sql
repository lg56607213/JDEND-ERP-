-- ============================================================================
-- Phase 2 - S1(v5): 차량 번호체계 컬럼 추가 + vehicle_mgmt_no UNIQUE 완화
-- ============================================================================
-- 변경 요청(설계 문서 Phase 2 확정 v5):
--   · 1발주=N대 지원 + 발주번호(J+YYMMDD+발주순번3) 도입 → order_no(10) 컬럼 추가.
--   · 발주(미실행) 차량관리번호 = 발주번호 + "000"(13자리). N대면 N행 모두 '...000' 동일값 공유
--     (제조사계약번호로 구분). 실번호(...001,...002)는 실행(S3)에서 확정.
--   · '...000' 공유 때문에 vehicle_mgmt_no 의 UNIQUE 제약을 제거한다.
--     (실행 후 유니크성은 실행 로직/앱에서 보장 — S3)
--   · 발주~선급(pre-실행) 단건 조회·이동은 행 PK(id)로 특정.
--
-- 적용 대상 DB (vehicle_orders 테이블이 있는 모든 DB):
--   - 템플릿 DB: erp   (⇐ 신규 회사 DB가 CREATE TABLE ... LIKE 로 이 스키마를 상속하므로 반드시 반영)
--   - 기존 회사 DB: erp_company_*   (각 DB마다 실행)
--
-- ★ 테스트 단계 권장(실데이터 없음): 기존 회사 테스트 DB(erp_company_*)는 아래 ALTER 대신
--   drop 후 재생성이 가장 깔끔하다. 엔티티에서 unique=true 를 제거했으므로 재생성되는 테이블엔
--   UNIQUE 인덱스가 아예 생기지 않고 order_no 도 상속된다(템플릿 erp 에 먼저 반영 필수).
-- ============================================================================

-- 1) 발주번호 컬럼 추가 -------------------------------------------------------
ALTER TABLE `erp`.`vehicle_orders`
  ADD COLUMN `order_no` VARCHAR(10) NULL AFTER `vehicle_mgmt_no`;

-- (선택) 그룹 조회 성능용 인덱스
-- ALTER TABLE `erp`.`vehicle_orders` ADD INDEX `idx_vehicle_orders_order_no` (`order_no`);

-- 2) vehicle_mgmt_no UNIQUE 제약 제거 ---------------------------------------
--   Hibernate(ddl-auto:update)가 만든 UNIQUE 인덱스 이름은 환경마다 다르다
--   (보통 `UK...` 해시 또는 `vehicle_mgmt_no`). 아래로 실제 인덱스명을 찾아 DROP 한다.
--
--   확인용:
--   SELECT index_name, non_unique FROM information_schema.statistics
--   WHERE table_schema='erp' AND table_name='vehicle_orders' AND column_name='vehicle_mgmt_no';
--
--   MySQL 8 동적 처리(유니크 인덱스명을 찾아 DROP):
SET @idx := (
  SELECT index_name FROM information_schema.statistics
  WHERE table_schema='erp' AND table_name='vehicle_orders'
    AND column_name='vehicle_mgmt_no' AND non_unique=0
  LIMIT 1
);
SET @sql := IF(@idx IS NULL, 'SELECT 1',
  CONCAT('ALTER TABLE `erp`.`vehicle_orders` DROP INDEX `', @idx, '`'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- (조회 성능용 비유니크 인덱스로 대체하고 싶으면)
-- ALTER TABLE `erp`.`vehicle_orders` ADD INDEX `idx_vehicle_orders_mgmt_no` (`vehicle_mgmt_no`);

-- 3) 기존 회사 DB에 반영 -----------------------------------------------------
--   회사 DB마다 위 1)·2)를 db명만 바꿔 실행. 테스트 DB는 drop 후 재생성 권장.
-- 예)
-- ALTER TABLE `erp_company_test111`.`vehicle_orders`
--   ADD COLUMN `order_no` VARCHAR(10) NULL AFTER `vehicle_mgmt_no`;
-- (그리고 위 2)의 동적 DROP 블록에서 table_schema='erp_company_test111' 로 바꿔 실행)

-- 참고: 기존 J0001 형식 데이터는 order_no 가 NULL 로 남는다(신규분부터 신규 포맷 적용).
