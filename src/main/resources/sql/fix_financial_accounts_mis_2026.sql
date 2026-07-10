-- 재무제표 계정 2건 수정
-- 1) 미상각잔액: 손익계산서 비용(판매비와관리비) 하위에 추가
--    (VehicleSaleService가 생성하는 미상각잔액 전표가 손익계산서에 집계되도록)
-- 2) 미지급금: 자산(ASSET) 분류로 잘못 등록된 행 삭제
--    (부채(LIABILITY) 쪽 정상 행은 유지, 자산 쪽 중복 행만 제거)
--
-- * 각 회사 DB(erp_company_a, erp_company_b, erp_company_c 등)에 모두 실행하세요.
-- * INSERT IGNORE → 이미 account_code='500214'가 있으면 무시(재실행 안전).
-- * DELETE → category='ASSET'인 미지급금만 삭제, 부채 쪽은 그대로.

-- 1. 미상각잔액 → 판매비와관리비(5002) 하위 소분류로 추가
INSERT IGNORE INTO financial_statement_accounts
  (statement_type, category, level, parent_id, account_code, account_name, account_type, display_order, is_active, is_postable)
SELECT 'is', 'EXPENSE', 3, id, '500214', '미상각잔액', '판매비와관리비', 14, '사용', '사용'
FROM financial_statement_accounts WHERE account_code = '5002';

-- 2. 자산으로 잘못 분류된 미지급금 삭제
DELETE FROM financial_statement_accounts
WHERE account_name = '미지급금' AND category = 'ASSET';
