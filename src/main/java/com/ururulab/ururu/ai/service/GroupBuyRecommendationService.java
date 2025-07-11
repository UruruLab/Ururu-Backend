package com.ururulab.ururu.ai.service;

import com.ururulab.ururu.ai.dto.GroupBuyRecommendationRequest;
import com.ururulab.ururu.ai.dto.GroupBuyRecommendationResponse;
import com.ururulab.ururu.ai.dto.GroupBuyRecommendationResponse.RecommendedGroupBuy;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class GroupBuyRecommendationService {

    private final GroupBuyRecommendationCacheService cacheService;
    private final UruruAiService aiService;
    private final BeautyProfileConversionService conversionService;

    public GroupBuyRecommendationResponse getRecommendationsByProfile(final Long memberId, final Integer topK) {
        log.info("뷰티프로필 기반 추천 요청 시작 - 회원ID: {}", memberId);

        final GroupBuyRecommendationRequest request = conversionService.convertToRecommendationRequest(memberId, topK);
        
        return getRecommendations(memberId, request);
    }

    public String checkAiServiceHealth() {
        try {
            log.info("AI 서비스 Health Check 시작");
            
            final long startTime = System.currentTimeMillis();
            final String healthStatus = aiService.checkHealth();
            final long responseTime = System.currentTimeMillis() - startTime;
            
            log.info("AI 서비스 Health Check 완료 - 응답시간: {}ms", responseTime);
            
            return String.format("AI 서비스 정상 (응답시간: %dms) - %s", responseTime, healthStatus);
            
        } catch (Exception e) {
            log.error("AI 서비스 Health Check 실패", e);
            return "AI 서비스 연결 실패: " + e.getMessage();
        }
    }

    public GroupBuyRecommendationResponse getRecommendations(final Long memberId, final GroupBuyRecommendationRequest request) {
        log.info("공동구매 추천 요청 시작 - 회원ID: {}, 피부타입: {}",
                memberId, request.beautyProfile().skinType());

        final GroupBuyRecommendationResponse cachedResponse = cacheService.getCachedRecommendation(memberId);
        if (cachedResponse != null) {
            log.info("캐시에서 추천 결과 반환 - 회원ID: {}", memberId);
            return updateCacheSource(cachedResponse, "CACHE");
        }

        if (!cacheService.tryAcquireProcessingLock(memberId)) {
            throw new BusinessException(ErrorCode.AI_RECOMMENDATION_PROCESSING_IN_PROGRESS);
        }

        try {
            final List<RecommendedGroupBuy> recommendations = aiService.getRecommendations(memberId, request);

            if (recommendations.isEmpty()) {
                throw new BusinessException(ErrorCode.AI_NO_RECOMMENDATIONS_FOUND);
            }

            final GroupBuyRecommendationResponse response = GroupBuyRecommendationResponse.of(
                    recommendations,
                    "AI가 분석한 맞춤형 추천입니다.", // TODO: AI 서비스에서 추천 이유 제공 시 수정
                    LocalDateTime.now(),
                    "AI_SERVICE"
            );

            cacheService.cacheRecommendation(memberId, response);

            log.info("공동구매 추천 완료 - 회원ID: {}, 추천 수: {}",
                    memberId, recommendations.size());

            return response;

        } finally {
            cacheService.releaseProcessingLock(memberId);
        }
    }

    @Async
    public CompletableFuture<Void> refreshRecommendationCache(final Long memberId, final GroupBuyRecommendationRequest request) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("백그라운드 추천 캐시 갱신 시작 - 회원ID: {}", memberId);

                final List<RecommendedGroupBuy> recommendations = aiService.getRecommendations(memberId, request);

                final GroupBuyRecommendationResponse response = GroupBuyRecommendationResponse.of(
                        recommendations,
                        "AI가 분석한 맞춤형 추천입니다.", // TODO: AI 서비스에서 추천 이유 제공 시 수정
                        LocalDateTime.now(),
                        "AI_SERVICE_REFRESH"
                );

                cacheService.cacheRecommendation(memberId, response);

                log.info("백그라운드 추천 캐시 갱신 완료 - 회원ID: {}", memberId);

            } catch (final Exception e) {
                log.error("백그라운드 추천 캐시 갱신 실패 - 회원ID: {}", memberId, e);
                throw new CompletionException(e);
            }
        });
    }

    public void evictRecommendationCache(final Long memberId) {
        log.info("추천 캐시 무효화 - 회원ID: {}", memberId);
        cacheService.evictMemberRecommendationCache(memberId);
    }

    public void evictAllRecommendationCaches() {
        log.info("전체 추천 캐시 무효화");
        cacheService.evictAllRecommendationCaches();
    }

    private GroupBuyRecommendationResponse updateCacheSource(
            final GroupBuyRecommendationResponse response,
            final String cacheSource
    ) {
        return GroupBuyRecommendationResponse.of(
                response.recommendedGroupBuys(),
                response.recommendationReason(),
                response.recommendedAt(),
                cacheSource
        );
    }
}
