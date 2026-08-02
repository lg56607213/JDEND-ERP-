-- ============================================================================
-- 전표 라인 계정코드 backfill
-- ----------------------------------------------------------------------------
-- 배경
--   재무제표 집계(StatementAggRepository)가 계정명 기준 → 계정코드 기준으로
--   바뀌면서 `l.account_code IS NOT NULL` 조건이 추가되었다.
--   그런데 자동전표를 만드는 4개 서비스(차량선급 / 보험 / 수납 / 법무)는
--   VoucherLine 을 직접 만들면서 account_name 만 채우고 account_code 를
--   비워 두었기 때문에, 승인된 전표인데도 재무제표에서 전부 누락되었다.
--
--   코드는 AccountResolver 도입으로 수정되었고, 이 스크립트는
--   그 전에 이미 쌓인 전표 라인의 계정코드를 계정명으로 역추적해 채운다.
--
-- 실행 순서
--   1) STEP 0 으로 대상 건수와 계정명을 먼저 확인한다.
--   2) 매칭되지 않는 계정명이 있으면 재무제표관리에 계정을 등록하거나
--      STEP 3 의 수동 매핑에 추가한다.
--   3) 백업 후 STEP 1 을 실행한다.
--   4) STEP 4 로 잔여 건을 재확인한다.
--
-- ※ 반드시 DB 백업 후 실행할 것.
-- ============================================================================


-- ---------------------------------------------------------------------------
-- STEP 0. 현황 파악 — 계정코드가 비어 재무제표에서 누락 중인 라인
-- ---------------------------------------------------------------------------
SELECT l.account_name,
       COUNT(*)        AS 건수,
       SUM(l.amount)   AS 합계금액,
       MIN(v.voucher_date) AS 최초일자,
       MAX(v.voucher_date) AS 최종일자,
       CASE WHEN a.account_code IS NULL THEN '계정 없음 → 수동확인 필요'
            ELSE CONCAT(a.account_code, ' 로 매칭됨')
       END AS 매칭결과
  FROM voucher_lines l
  JOIN vouchers v ON v.id = l.voucher_id
  LEFT JOIN financial_statement_accounts a
         ON a.account_name = l.account_name
 WHERE l.account_code IS NULL
 GROUP BY l.account_name, a.account_code
 ORDER BY 건수 DESC;


-- ---------------------------------------------------------------------------
-- STEP 1. 계정명이 재무제표관리와 정확히 일치하는 건 채우기
-- ---------------------------------------------------------------------------
UPDATE voucher_lines l
  JOIN financial_statement_accounts a
    ON a.account_name = l.account_name
   SET l.account_code = a.account_code
 WHERE l.account_code IS NULL;


-- ---------------------------------------------------------------------------
-- STEP 2. 앞뒤 공백 때문에 안 맞은 건 채우기
-- ---------------------------------------------------------------------------
UPDATE voucher_lines l
  JOIN financial_statement_accounts a
    ON TRIM(a.account_name) = TRIM(l.account_name)
   SET l.account_code = a.account_code
 WHERE l.account_code IS NULL;


-- ---------------------------------------------------------------------------
-- STEP 3. 이름이 달라 자동 매칭이 안 되는 건 — 필요한 것만 주석 해제해 사용
--         (STEP 0 결과에서 '계정 없음' 으로 나온 계정명을 여기에 매핑)
-- ---------------------------------------------------------------------------
-- UPDATE voucher_lines SET account_code = '400101'
--  WHERE account_code IS NULL AND account_name = '임대수익';      -- → 렌트수익
-- UPDATE voucher_lines SET account_code = '100101'
--  WHERE account_code IS NULL AND account_name = '보통예금';
-- UPDATE voucher_lines SET account_code = '200102'
--  WHERE account_code IS NULL AND account_name = '선수금';


-- ---------------------------------------------------------------------------
-- STEP 4. 남은 건 확인 — 0건이어야 정상
-- ---------------------------------------------------------------------------
SELECT l.account_name, COUNT(*) AS 남은건수, SUM(l.amount) AS 합계금액
  FROM voucher_lines l
 WHERE l.account_code IS NULL
 GROUP BY l.account_name
 ORDER BY 남은건수 DESC;


-- ---------------------------------------------------------------------------
-- STEP 5. 채워진 코드가 재무제표관리에 실제로 있고 '사용' 상태인지 확인
--         (미등록이거나 '미사용' 이면 여전히 재무제표에 안 잡힌다)
-- ---------------------------------------------------------------------------
SELECT l.account_code,
       l.account_name,
       COUNT(*) AS 건수,
       CASE WHEN a.account_code IS NULL THEN '재무제표관리에 없음'
            WHEN a.is_active <> '사용'  THEN '미사용 상태'
            ELSE '정상'
       END AS 상태
  FROM voucher_lines l
  LEFT JOIN financial_statement_accounts a
         ON a.account_code = l.account_code
 WHERE l.account_code IS NOT NULL
 GROUP BY l.account_code, l.account_name, a.account_code, a.is_active
HAVING 상태 <> '정상'
 ORDER BY 건수 DESC;
