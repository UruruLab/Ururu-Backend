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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class GroupBuyRecommendationService {

    private final GroupBuyRecommendationCacheService cacheService;
    private final UruruAiService aiService;

    public GroupBuyRecommendationResponse getRecommendations(final GroupBuyRecommendationRequest request) {
        log.info("공동구매 추천 요청 시작 - 회원ID: {}, 피부타입: {}",
                request.memberId(), request.skinType());

        final GroupBuyRecommendationResponse cachedResponse = cacheService.getCachedRecommendation(request.memberId());
        if (cachedResponse != null) {
            log.info("캐시에서 추천 결과 반환 - 회원ID: {}", request.memberId());
            return updateCacheSource(cachedResponse, "CACHE");
        }

        if (!cacheService.tryAcquireProcessingLock(request.memberId())) {
            throw new BusinessException(ErrorCode.ORDER_PROCESSING_IN_PROGRESS);
        }

        try {
            final List<RecommendedGroupBuy> recommendations = aiService.getRecommendations(request);

            if (recommendations.isEmpty()) {
                throw new BusinessException(ErrorCode.AI_NO_RECOMMENDATIONS_FOUND);
            }

            final GroupBuyRecommendationResponse response = GroupBuyRecommendationResponse.of(
                    recommendations,
                    buildRecommendationReason(request),
                    LocalDateTime.now(),
                    "AI_SERVICE"
            );

            cacheService.cacheRecommendation(request.memberId(), response);

            log.info("공동구매 추천 완료 - 회원ID: {}, 추천 수: {}",
                    request.memberId(), recommendations.size());

            return response;

        } finally {
            cacheService.releaseProcessingLock(request.memberId());
        }
    }

    @Async
    public CompletableFuture<Void> refreshRecommendationCache(final GroupBuyRecommendationRequest request) {
        try {
            log.info("백그라운드 추천 캐시 갱신 시작 - 회원ID: {}", request.memberId());

            final List<RecommendedGroupBuy> recommendations = aiService.getRecommendations(request);

            final GroupBuyRecommendationResponse response = GroupBuyRecommendationResponse.of(
                    recommendations,
                    buildRecommendationReason(request),
                    LocalDateTime.now(),
                    "AI_SERVICE_REFRESH"
            );

            cacheService.cacheRecommendation(request.memberId(), response);

            log.info("백그라운드 추천 캐시 갱신 완료 - 회원ID: {}", request.memberId());

        } catch (final Exception e) {
            log.error("백그라운드 추천 캐시 갱신 실패 - 회원ID: {}", request.memberId(), e);
        }

        return CompletableFuture.completedFuture(null);
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

    private String buildRecommendationReason(final GroupBuyRecommendationRequest request) {
        final StringBuilder reason = new StringBuilder();

        reason.append(request.skinType()).append(" 피부 타입");

        if (request.skinConcerns() != null && !request.skinConcerns().isEmpty()) {
            reason.append("과 ").append(String.join(", ", request.skinConcerns())).append(" 고민");
        }

        reason.append("을 고려하여 개인 맞춤형 공동구매 상품을 추천해드렸습니다.");

        if (request.preferredCategories() != null && !request.preferredCategories().isEmpty()) {
            reason.append(" 선호하시는 ").append(String.join(", ", request.preferredCategories()))
                    .append(" 카테고리를 우선적으로 반영했습니다.");
        }

        return reason.toString();
    }
}
