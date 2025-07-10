package com.ururulab.ururu.ai.service;

import com.ururulab.ururu.ai.dto.GroupBuyRecommendationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupBuyRecommendationCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String AI_PREFIX = "ai:";
    private static final String GROUPBUY_RECOMMENDATION_PREFIX = AI_PREFIX + "groupbuy:recommendation:member:";
    private static final String POPULAR_GROUPBUY_RANKING_KEY = AI_PREFIX + "groupbuy:popular:ranking";
    private static final String PROCESSING_LOCK_PREFIX = AI_PREFIX + "processing:member:";

    private static final Duration RECOMMENDATION_TTL = Duration.ofMinutes(30);
    private static final Duration POPULAR_RANKING_TTL = Duration.ofMinutes(10);
    private static final Duration PROCESSING_LOCK_TTL = Duration.ofMinutes(2);

    public GroupBuyRecommendationResponse getCachedRecommendation(final Long memberId) {
        final String key = buildRecommendationKey(memberId);

        try {
            final GroupBuyRecommendationResponse cached = (GroupBuyRecommendationResponse) redisTemplate.opsForValue().get(key);

            if (cached != null) {
                log.debug("캐시에서 추천 결과 조회 성공 - 회원ID: {}", memberId);
                return cached;
            }

            log.debug("캐시에 추천 결과 없음 - 회원ID: {}", memberId);
            return null;

        } catch (final Exception e) {
            log.warn("캐시 조회 중 오류 발생 - 회원ID: {}", memberId, e);
            return null;
        }
    }

    public void cacheRecommendation(final Long memberId, final GroupBuyRecommendationResponse response) {
        final String key = buildRecommendationKey(memberId);

        try {
            redisTemplate.opsForValue().set(key, response, RECOMMENDATION_TTL);
            log.debug("추천 결과 캐시 저장 완료 - 회원ID: {}, TTL: {}분", memberId, RECOMMENDATION_TTL.toMinutes());

        } catch (final Exception e) {
            log.warn("캐시 저장 중 오류 발생 - 회원ID: {}", memberId, e);
        }
    }

    public void evictMemberRecommendationCache(final Long memberId) {
        final String key = buildRecommendationKey(memberId);

        try {
            final Boolean deleted = redisTemplate.delete(key);

            if (Boolean.TRUE.equals(deleted)) {
                log.debug("회원 추천 캐시 삭제 완료 - 회원ID: {}", memberId);
            } else {
                log.debug("삭제할 추천 캐시 없음 - 회원ID: {}", memberId);
            }

        } catch (final Exception e) {
            log.warn("캐시 삭제 중 오류 발생 - 회원ID: {}", memberId, e);
        }
    }

    public void evictAllRecommendationCaches() {
        try {
            final String pattern = GROUPBUY_RECOMMENDATION_PREFIX + "*";
            final Set<String> keys = redisTemplate.keys(pattern);

            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("전체 추천 캐시 삭제 완료 - 삭제된 키 수: {}", keys.size());
            } else {
                log.debug("삭제할 추천 캐시 없음");
            }

        } catch (final Exception e) {
            log.warn("전체 캐시 삭제 중 오류 발생", e);
        }
    }

    public boolean tryAcquireProcessingLock(final Long memberId) {
        final String key = PROCESSING_LOCK_PREFIX + memberId;

        try {
            final Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "processing", PROCESSING_LOCK_TTL);

            if (Boolean.TRUE.equals(success)) {
                log.debug("AI 추천 처리 락 획득 성공 - 회원ID: {}", memberId);
                return true;
            } else {
                log.debug("AI 추천 처리 락 획득 실패 (중복 요청) - 회원ID: {}", memberId);
                return false;
            }

        } catch (final Exception e) {
            log.warn("처리 락 획득 중 오류 발생 - 회원ID: {}", memberId, e);
            return false;
        }
    }

    public void releaseProcessingLock(final Long memberId) {
        final String key = PROCESSING_LOCK_PREFIX + memberId;

        try {
            redisTemplate.delete(key);
            log.debug("AI 추천 처리 락 해제 - 회원ID: {}", memberId);

        } catch (final Exception e) {
            log.warn("처리 락 해제 중 오류 발생 - 회원ID: {}", memberId, e);
        }
    }

    public void cachePopularGroupBuyRanking(final Object ranking) {
        try {
            redisTemplate.opsForValue().set(POPULAR_GROUPBUY_RANKING_KEY, ranking, POPULAR_RANKING_TTL);
            log.debug("인기 공동구매 랭킹 캐시 저장 완료 - TTL: {}분", POPULAR_RANKING_TTL.toMinutes());

        } catch (final Exception e) {
            log.warn("인기 공동구매 랭킹 캐시 저장 중 오류 발생", e);
        }
    }

    public Object getCachedPopularGroupBuyRanking() {
        try {
            final Object cached = redisTemplate.opsForValue().get(POPULAR_GROUPBUY_RANKING_KEY);

            if (cached != null) {
                log.debug("캐시에서 인기 공동구매 랭킹 조회 성공");
                return cached;
            }

            log.debug("캐시에 인기 공동구매 랭킹 없음");
            return null;

        } catch (final Exception e) {
            log.warn("인기 공동구매 랭킹 캐시 조회 중 오류 발생", e);
            return null;
        }
    }

    private String buildRecommendationKey(final Long memberId) {
        return GROUPBUY_RECOMMENDATION_PREFIX + memberId;
    }
}
