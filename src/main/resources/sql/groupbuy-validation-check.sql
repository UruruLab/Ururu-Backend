-- =====================================================
-- 공동구매 DB 초기화 실행 전 검증 스크립트
-- 실행 환경: MySQL 8.0 (AWS RDS)
-- 생성일: 2025-07-12
-- 목적: 안전한 스크립트 실행을 위한 사전 검증
-- =====================================================

SELECT '🔍 공동구매 DB 초기화 실행 전 검증 시작' as status;

-- =====================================================
-- 1. 기존 데이터 확인
-- =====================================================
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN CONCAT('⚠️ 기존 공동구매 데이터 존재: ', COUNT(*), '개')
        ELSE '✅ 기존 공동구매 데이터 없음 - 안전하게 실행 가능'
    END as existing_groupbuy_check,
    COUNT(*) as current_count
FROM groupbuys;

SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN CONCAT('⚠️ 기존 공동구매 옵션 데이터 존재: ', COUNT(*), '개')
        ELSE '✅ 기존 공동구매 옵션 데이터 없음 - 안전하게 실행 가능'
    END as existing_options_check,
    COUNT(*) as current_count
FROM groupbuy_options;

-- =====================================================
-- 2. 상품 데이터 충분성 검증
-- =====================================================
SELECT 
    CASE 
        WHEN COUNT(*) >= 400 THEN CONCAT('✅ 충분한 활성 상품 존재: ', COUNT(*), '개')
        ELSE CONCAT('❌ 활성 상품 부족: ', COUNT(*), '개 (400개 필요)')
    END as product_availability_check,
    COUNT(*) as active_product_count
FROM products 
WHERE status = 'ACTIVE' AND name IS NOT NULL;

-- =====================================================
-- 3. 상품 옵션 데이터 검증
-- =====================================================
SELECT 
    CASE 
        WHEN COUNT(*) >= 400 THEN CONCAT('✅ 충분한 상품 옵션 존재: ', COUNT(*), '개')
        ELSE CONCAT('❌ 상품 옵션 부족: ', COUNT(*), '개 (400개 필요)')
    END as product_option_check,
    COUNT(*) as active_option_count
FROM product_options po
JOIN products p ON po.product_id = p.id
WHERE p.status = 'ACTIVE' AND po.is_deleted = 0;

-- =====================================================
-- 4. 판매자 데이터 검증
-- =====================================================
SELECT 
    CASE 
        WHEN COUNT(*) >= 10 THEN CONCAT('✅ 충분한 활성 판매자 존재: ', COUNT(*), '개')
        ELSE CONCAT('❌ 활성 판매자 부족: ', COUNT(*), '개 (10개 권장)')
    END as seller_check,
    COUNT(*) as active_seller_count
FROM sellers 
WHERE is_deleted = 0;

-- =====================================================
-- 5. 카테고리별 상품 분포 확인
-- =====================================================
SELECT 
    '📊 카테고리별 상품 분포' as category_distribution,
    '' as spacer;

SELECT 
    c.id as category_id,
    c.name as category_name,
    COUNT(DISTINCT p.id) as product_count,
    CASE 
        WHEN COUNT(DISTINCT p.id) >= 70 THEN '✅ 충분'
        WHEN COUNT(DISTINCT p.id) >= 50 THEN '⚠️ 보통'
        ELSE '❌ 부족'
    END as availability_status
FROM products p
JOIN product_categories pc ON p.id = pc.product_id
JOIN categories c ON pc.category_id = c.id
WHERE p.status = 'ACTIVE'
GROUP BY c.id, c.name
ORDER BY product_count DESC
LIMIT 10;

-- =====================================================
-- 6. 테이블 제약조건 확인
-- =====================================================
SELECT 
    TABLE_NAME,
    CONSTRAINT_NAME,
    CONSTRAINT_TYPE
FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('groupbuys', 'groupbuy_options')
ORDER BY TABLE_NAME, CONSTRAINT_TYPE;

-- =====================================================
-- 7. 최종 실행 권장 여부
-- =====================================================
SELECT 
    CASE 
        WHEN (SELECT COUNT(*) FROM groupbuys) = 0 AND 
             (SELECT COUNT(*) FROM products WHERE status = 'ACTIVE') >= 400 AND
             (SELECT COUNT(*) FROM product_options po JOIN products p ON po.product_id = p.id WHERE p.status = 'ACTIVE' AND po.is_deleted = 0) >= 400 AND
             (SELECT COUNT(*) FROM sellers WHERE is_deleted = 0) >= 5
        THEN '✅ 모든 조건 만족 - 안전하게 실행 가능'
        WHEN (SELECT COUNT(*) FROM groupbuys) > 0
        THEN '⚠️ 기존 데이터 존재 - 백업 후 실행 권장'
        ELSE '❌ 실행 전 조건 확인 필요 - 상품/판매자 데이터 부족'
    END as final_execution_recommendation;

-- =====================================================
-- 8. 예상 실행 시간 및 리소스 사용량 안내
-- =====================================================
SELECT 
    '⏰ 예상 실행 정보' as execution_info,
    '약 2-5분 소요 예상 (상품 수에 따라 변동)' as estimated_time,
    '트랜잭션 사용으로 롤백 가능' as safety_info,
    '백업 테이블 자동 생성' as backup_info;

SELECT '🔍 검증 완료 - 위 결과를 확인 후 메인 스크립트 실행하세요' as completion_message;
