-- ============================================================================
-- 세금계산서 공급자(자사) 정보 테이블 추가: my_supplier_info
-- ============================================================================
-- 배경/변경 요청:
--   기존 TaxInvoiceService 에 공급자 정보(사업자등록번호·상호 등)가 하드코딩되어
--   있어(예: "0000000000", "주식회사 제이디엔드렌트카") 멀티회사(회사별 DB) SaaS인데도
--   모든 테넌트의 전자세금계산서에 JDEND 자사 정보가 찍히는 버그가 있었다.
--   이를 회사(테넌트)별로 '내정보관리'에서 입력한 값으로 대체한다.
--
-- 저장 필드(테넌트당 1건, 싱글턴 성격):
--   ① 상호명(company_name) ② 대표자명(ceo_name) ③ 업태(business_type)
--   ④ 업종/종목(business_item) ⑤ 이메일(email)
--   ⑥ 사업자등록번호(registration_number) ⑦ 사업장주소(address)
--   ※ ⑥·⑦ 은 사장 요청 5개(①~⑤)에 더해, 세금계산서 법적 필수 항목이라 함께 추가.
--
-- 적용 대상 DB (전자세금계산서 기능을 쓰는 모든 DB):
--   - 템플릿 DB: erp   (⇐ 신규 회사 DB가 CREATE TABLE ... LIKE 로 이 스키마를 상속하므로 반드시 반영)
--   - 기존 회사 DB: erp_company_*   (각 DB마다 실행)
--
-- ★ 테스트 단계 권장(실데이터 없음): 기존 회사 테스트 DB(erp_company_*)는 아래 CREATE 대신
--   drop 후 재생성이 가장 깔끔하다(템플릿 erp 에 먼저 반영 필수). 엔티티(ddl-auto:update)로도
--   테이블이 생성될 수 있으나, 신규 테넌트가 CREATE TABLE LIKE 로 상속하려면 템플릿 erp 에
--   반드시 이 테이블이 존재해야 하므로 아래 스크립트를 템플릿 erp 에 먼저 실행한다.
-- ============================================================================

-- 1) 템플릿 DB(erp) 에 테이블 생성 -------------------------------------------
CREATE TABLE IF NOT EXISTS `erp`.`my_supplier_info` (
  `id`                  BIGINT       NOT NULL AUTO_INCREMENT,
  `company_name`        VARCHAR(100) NULL,
  `ceo_name`            VARCHAR(50)  NULL,
  `business_type`       VARCHAR(100) NULL,
  `business_item`       VARCHAR(100) NULL,
  `email`               VARCHAR(100) NULL,
  `registration_number` VARCHAR(20)  NULL,
  `address`             VARCHAR(255) NULL,
  `created_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2) 기존 회사 DB에 반영 -----------------------------------------------------
--   회사 DB마다 위 1)을 db명만 바꿔 실행. 테스트 DB는 drop 후 재생성 권장.
-- 예)
-- CREATE TABLE IF NOT EXISTS `erp_company_test111`.`my_supplier_info` LIKE `erp`.`my_supplier_info`;
-- CREATE TABLE IF NOT EXISTS `erp_company_test112`.`my_supplier_info` LIKE `erp`.`my_supplier_info`;
-- CREATE TABLE IF NOT EXISTS `erp_company_test113`.`my_supplier_info` LIKE `erp`.`my_supplier_info`;

-- 참고: 데이터는 테넌트가 '내정보관리 > 세금계산서 공급자 정보'에서 직접 입력한다.
--       미입력 시 전자세금계산서 공급자 칸은 빈 값으로 안전 출력된다(크래시 없음).
