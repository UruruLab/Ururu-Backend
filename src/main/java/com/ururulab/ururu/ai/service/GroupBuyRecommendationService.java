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
    private final AiRecommendationService aiRecommendationService;
    private final BeautyProfileConversionService conversionService;
    private final GroupBuyRecommendationRequestProcessor requestProcessor;

    public GroupBuyRecommendationResponse getRecommendationsByProfile(final Long memberId, final Integer topK) {
        log.info("뷰티프로필 기반 추천 요청 시작 - 회원ID: {}", memberId);

        final GroupBuyRecommendationRequest request = conversionService.convertToRecommendationRequest(memberId, topK);
        
        return getRecommendations(memberId, request);
    }

    public String checkAiServiceHealth() {
        return aiRecommendationService.checkHealth();
    }

    public GroupBuyRecommendationResponse getRecommendations(final Long memberId, final GroupBuyRecommendationRequest request) {
        log.info("공동구매 추천 요청 시작 - 회원ID: {}, 피부타입: {}",
                memberId, request.beautyProfile().skinType());

        // 기본값 적용
        final GroupBuyRecommendationRequest processedRequest = requestProcessor.applyDefaults(request);
        
        log.debug("기본값 적용 완료 - topK: {}, minSimilarity: {}, usePriceFilter: {}",
                processedRequest.topK(), processedRequest.minSimilarity(), processedRequest.usePriceFilter());

        final GroupBuyRecommendationResponse cachedResponse = cacheService.getCachedRecommendation(memberId);
        if (cachedResponse != null) {
            log.info("캐시에서 추천 결과 반환 - 회원ID: {}", memberId);
            return updateCacheSource(cachedResponse, "CACHE");
        }

        if (!cacheService.tryAcquireProcessingLock(memberId)) {
            throw new BusinessException(ErrorCode.AI_RECOMMENDATION_PROCESSING_IN_PROGRESS);
        }

        try {
            final List<RecommendedGroupBuy> recommendations = aiRecommendationService.getRecommendations(memberId, processedRequest);

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

                // 기본값 적용
                final GroupBuyRecommendationRequest processedRequest = requestProcessor.applyDefaults(request);

                final List<RecommendedGroupBuy> recommendations = aiRecommendationService.getRecommendations(memberId, processedRequest);

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
