-- =====================================================
-- 공동구매 DB 초기화 롤백 스크립트
-- 실행 환경: MySQL 8.0 (AWS RDS)
-- 생성일: 2025-07-12
-- 목적: 문제 발생 시 안전한 데이터 복구
-- =====================================================

-- 이 스크립트는 groupbuy-initial-data-setup.sql 실행 후 
-- 문제가 발생했을 때만 사용하세요!

SELECT '⚠️ 공동구매 데이터 롤백 시작' as status;

-- =====================================================
-- 1. 현재 상태 확인
-- =====================================================
SELECT 'groupbuys 현재 데이터' as table_name, COUNT(*) as count FROM groupbuys
UNION ALL
SELECT 'groupbuy_options 현재 데이터' as table_name, COUNT(*) as count FROM groupbuy_options
UNION ALL
SELECT 'groupbuys_backup_20250712 백업 데이터' as table_name, COUNT(*) as count FROM groupbuys_backup_20250712
UNION ALL
SELECT 'groupbuy_options_backup_20250712 백업 데이터' as table_name, COUNT(*) as count FROM groupbuy_options_backup_20250712;

-- =====================================================
-- 2. 백업 테이블 존재 여부 확인
-- =====================================================
SELECT 
    CASE 
        WHEN (SELECT COUNT(*) FROM information_schema.tables 
              WHERE table_schema = DATABASE() 
              AND table_name = 'groupbuys_backup_20250712') > 0
        THEN '✅ groupbuys 백업 테이블 존재'
        ELSE '❌ groupbuys 백업 테이블 없음 - 롤백 불가'
    END as groupbuys_backup_check
UNION ALL
SELECT 
    CASE 
        WHEN (SELECT COUNT(*) FROM information_schema.tables 
              WHERE table_schema = DATABASE() 
              AND table_name = 'groupbuy_options_backup_20250712') > 0
        THEN '✅ groupbuy_options 백업 테이블 존재'
        ELSE '❌ groupbuy_options 백업 테이블 없음 - 롤백 불가'
    END as groupbuy_options_backup_check;

-- =====================================================
-- 3. 롤백 실행 (수동으로 주석 해제)
-- =====================================================

-- 안전장치: 트랜잭션 시작
-- START TRANSACTION;

-- 현재 데이터 삭제
-- DELETE FROM groupbuy_options;
-- DELETE FROM groupbuys;

-- 백업에서 복원
-- INSERT INTO groupbuys SELECT * FROM groupbuys_backup_20250712;
-- INSERT INTO groupbuy_options SELECT * FROM groupbuy_options_backup_20250712;

-- 복원 확인
-- SELECT 'groupbuys 복원 후' as table_name, COUNT(*) as count FROM groupbuys
-- UNION ALL
-- SELECT 'groupbuy_options 복원 후' as table_name, COUNT(*) as count FROM groupbuy_options;

-- 복원 성공 시 커밋
-- COMMIT;

-- =====================================================
-- 4. 백업 테이블 정리 (복원 성공 후 실행)
-- =====================================================

-- 백업 테이블 삭제 (선택사항 - 보관하려면 주석 유지)
-- DROP TABLE IF EXISTS groupbuys_backup_20250712;
-- DROP TABLE IF EXISTS groupbuy_options_backup_20250712;

-- =====================================================
-- 5. 완전 초기화 (백업도 없을 때 - 신중히 사용)
-- =====================================================

-- 모든 공동구매 데이터 삭제 (백업 없이)
-- 이 작업은 되돌릴 수 없습니다!
-- START TRANSACTION;
-- DELETE FROM groupbuy_options;
-- DELETE FROM groupbuys;
-- 
-- 삭제 확인
-- SELECT COUNT(*) as remaining_groupbuys FROM groupbuys;
-- SELECT COUNT(*) as remaining_options FROM groupbuy_options;
-- 
-- 확인 후 커밋
-- COMMIT;

-- =====================================================
-- 6. 롤백 완료 안내
-- =====================================================
SELECT 
    '🔄 롤백 스크립트 준비 완료' as status,
    '위의 주석을 해제하여 단계별로 실행하세요' as instruction,
    '1. 백업 확인 → 2. 트랜잭션 시작 → 3. 데이터 삭제 → 4. 백업 복원 → 5. 커밋' as steps;

-- =====================================================
-- 7. 긴급 롤백 (한 번에 실행)
-- =====================================================
-- 문제 발생 시 아래 주석을 모두 해제하고 실행

/*
START TRANSACTION;

-- 현재 데이터 삭제
DELETE FROM groupbuy_options;
DELETE FROM groupbuys;

-- 백업에서 복원
INSERT INTO groupbuys SELECT * FROM groupbuys_backup_20250712;
INSERT INTO groupbuy_options SELECT * FROM groupbuy_options_backup_20250712;

-- 복원 검증
SELECT 
    CASE 
        WHEN (SELECT COUNT(*) FROM groupbuys) = (SELECT COUNT(*) FROM groupbuys_backup_20250712) AND
             (SELECT COUNT(*) FROM groupbuy_options) = (SELECT COUNT(*) FROM groupbuy_options_backup_20250712)
        THEN '✅ 롤백 성공'
        ELSE '❌ 롤백 실패 - 수동 확인 필요'
    END as rollback_result;

COMMIT;
*/