package com.ururulab.ururu.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

/**
 * Redis 기반 재고 예약 서비스
 * 주문서 생성 시 임시 재고 차감 및 TTL 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockReservationService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String RESERVATION_KEY_PREFIX = "pending_order:";
    private static final String PROCESSING_KEY_PREFIX = "processing:";
    private static final Duration RESERVATION_TTL = Duration.ofMinutes(30);
    private static final Duration PROCESSING_TTL = Duration.ofMinutes(5);

    /**
     * 재고 예약 생성
     * @param optionId 공구 옵션 ID
     * @param memberId 회원 ID
     * @param quantity 예약 수량
     */
    public void reserveStock(Long optionId, Long memberId, Integer quantity) {
        String key = buildReservationKey(optionId, memberId);
        redisTemplate.opsForValue().set(key, quantity.toString(), RESERVATION_TTL);

        log.debug("재고 예약 생성 - 옵션ID: {}, 회원ID: {}, 수량: {}", optionId, memberId, quantity);
    }

    /**
     * 재고 예약 해제
     * @param optionId 공구 옵션 ID
     * @param memberId 회원 ID
     */
    public void releaseReservation(Long optionId, Long memberId) {
        String key = buildReservationKey(optionId, memberId);
        redisTemplate.delete(key);

        log.debug("재고 예약 해제 - 옵션ID: {}, 회원ID: {}", optionId, memberId);
    }

    /**
     * 특정 옵션의 총 예약 수량 조회
     * @param optionId 공구 옵션 ID
     * @return 총 예약 수량
     */
    public Integer getTotalReservedQuantity(Long optionId) {
        String pattern = RESERVATION_KEY_PREFIX + optionId + ":*";
        Set<String> keys = redisTemplate.keys(pattern);

        if (keys == null || keys.isEmpty()) {
            return 0;
        }

        return keys.stream()
                .mapToInt(key -> {
                    String value = redisTemplate.opsForValue().get(key);
                    return value != null ? Integer.parseInt(value) : 0;
                })
                .sum();
    }

    /**
     * 특정 회원의 특정 옵션 예약 수량 조회
     * @param optionId 공구 옵션 ID
     * @param memberId 회원 ID
     * @return 예약 수량 (없으면 0)
     */
    public Integer getReservedQuantity(Long optionId, Long memberId) {
        String key = buildReservationKey(optionId, memberId);
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Integer.parseInt(value) : 0;
    }

    /**
     * 사용자별 중복 요청 방지 락
     * @param memberId 회원 ID
     * @return 락 획득 성공 여부
     */
    public boolean tryAcquireProcessingLock(Long memberId) {
        String key = PROCESSING_KEY_PREFIX + memberId;
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "processing", PROCESSING_TTL);

        if (Boolean.TRUE.equals(success)) {
            log.debug("처리 락 획득 성공 - 회원ID: {}", memberId);
            return true;
        } else {
            log.debug("처리 락 획득 실패 (중복 요청) - 회원ID: {}", memberId);
            return false;
        }
    }

    /**
     * 사용자별 중복 요청 방지 락 해제
     * @param memberId 회원 ID
     */
    public void releaseProcessingLock(Long memberId) {
        String key = PROCESSING_KEY_PREFIX + memberId;
        redisTemplate.delete(key);

        log.debug("처리 락 해제 - 회원ID: {}", memberId);
    }

    /**
     * 회원의 기존 예약들 모두 해제 (재주문 시 사용)
     * @param memberId 회원 ID
     */
    public void releaseAllUserReservations(Long memberId) {
        String pattern = RESERVATION_KEY_PREFIX + "*:" + memberId;
        Set<String> keys = redisTemplate.keys(pattern);

        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.debug("사용자 모든 예약 해제 - 회원ID: {}, 해제된 예약 수: {}", memberId, keys.size());
        }
    }

    private String buildReservationKey(Long optionId, Long memberId) {
        return RESERVATION_KEY_PREFIX + optionId + ":" + memberId;
    }
}