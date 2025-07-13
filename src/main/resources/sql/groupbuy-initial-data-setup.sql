-- =====================================================
-- 공동구매 DB 초기 데이터 세팅 스크립트 (중복 없는 상품 기반)
-- MySQL 8.0 (AWS RDS)
-- 목적: 랭킹/추천 시스템 테스트, 운영 데이터 초기화
-- =====================================================

START TRANSACTION;

-- 백업 테이블 생성 (롤백 대비)
CREATE TABLE IF NOT EXISTS groupbuys_backup_20250712 AS SELECT * FROM groupbuys;
CREATE TABLE IF NOT EXISTS groupbuy_options_backup_20250712 AS SELECT * FROM groupbuy_options;

-- 필요시 기존 데이터 삭제 (주석 해제 후 사용)
-- DELETE FROM groupbuy_options;
-- DELETE FROM groupbuys;

-- =====================================================
-- 1. 상품 1개당 공동구매 1개(중복 없이) 생성
-- =====================================================
INSERT INTO groupbuys (
    product_id, seller_id, title, description, thumbnail_url, thumbnail_hash,
    discount_stages, max_discount_rate, limit_quantity_per_member, status,
    start_at, ends_at, display_final_price, created_at, updated_at
)
SELECT
    p.id as product_id,
    p.seller_id as seller_id,
    CONCAT('공동구매 - ', p.name) as title,
    CONCAT(p.name, ' 공동구매를 시작합니다!') as description,
    MAX(po.image_url) as thumbnail_url,
    SHA2(COALESCE(MAX(po.image_url), ''), 256) as thumbnail_hash,
    '{"stages":[{"quantity":10,"rate":10},{"quantity":30,"rate":20}]}' as discount_stages,
    20 as max_discount_rate,
    3 as limit_quantity_per_member,
    'OPEN' as status,
    NOW() as start_at,
    DATE_ADD(NOW(), INTERVAL 7 DAY) as ends_at,
    MAX(po.price) as display_final_price,
    NOW() as created_at,
    NOW() as updated_at
FROM products p
JOIN product_options po ON p.id = po.product_id AND po.is_deleted = 0
WHERE p.status = 'ACTIVE'
  AND p.id NOT IN (SELECT product_id FROM groupbuys)
GROUP BY p.id, p.seller_id, p.name
ORDER BY RAND()
LIMIT 400;

-- =====================================================
-- 2. 각 공동구매별 옵션 1개씩 생성 (CodeRabbit 개선사항 적용)
-- =====================================================
-- 새로 생성된 GroupBuy ID들을 임시 테이블로 관리
CREATE TEMPORARY TABLE tmp_new_groupbuy_ids AS
SELECT id FROM groupbuys WHERE created_at >= (SELECT MIN(created_at) FROM groupbuys WHERE created_at >= NOW() - INTERVAL 1 MINUTE);

INSERT INTO groupbuy_options (
    initial_stock, price_override, sale_price, stock,
    created_at, groupbuy_id, product_option_id, updated_at
)
SELECT
    100 as initial_stock,
    po.price as price_override,
    po.price as sale_price,
    100 as stock,
    NOW() as created_at,
    gb.id as groupbuy_id,
    po.id as product_option_id,
    NOW() as updated_at
FROM groupbuys gb
JOIN tmp_new_groupbuy_ids t ON gb.id = t.id
JOIN products p ON gb.product_id = p.id
JOIN product_options po ON p.id = po.product_id AND po.is_deleted = 0
GROUP BY gb.id
ORDER BY gb.id;

-- =====================================================
-- 3. 데이터 검증 및 통계
-- =====================================================
SELECT 'GroupBuy 총 개수' as metric, COUNT(*) as count FROM groupbuys
UNION ALL
SELECT 'GroupBuyOption 총 개수', COUNT(*) FROM groupbuy_options;

-- =====================================================
-- 4. 최종 검증
-- =====================================================
SELECT 
    CASE 
        WHEN (SELECT COUNT(*) FROM groupbuys) = 400 AND 
             (SELECT COUNT(*) FROM groupbuy_options) >= 400
        THEN '✅ VALIDATION PASSED - 모든 조건 만족'
        ELSE CONCAT('❌ 공동구매/옵션 개수 불일치: ', (SELECT COUNT(*) FROM groupbuys), '/', (SELECT COUNT(*) FROM groupbuy_options))
    END as final_validation_result;

-- =====================================================
-- 커밋 또는 롤백 안내
-- =====================================================
-- 문제가 없으면 COMMIT; 입력
-- 문제가 있으면 ROLLBACK; 입력