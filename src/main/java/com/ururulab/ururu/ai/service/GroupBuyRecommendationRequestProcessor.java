package com.ururulab.ururu.ai.service;

import com.ururulab.ururu.ai.config.AiRecommendationProperties;
import com.ururulab.ururu.ai.dto.GroupBuyRecommendationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * AI 추천 요청 전처리 서비스.
 * null 값에 대한 기본값 적용 및 요청 검증을 담당합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GroupBuyRecommendationRequestProcessor {

    private final AiRecommendationProperties aiProperties;

    /**
     * 요청에 기본값을 적용합니다.
     * 
     * @param request 원본 요청
     * @return 기본값이 적용된 요청
     */
    public GroupBuyRecommendationRequest applyDefaults(final GroupBuyRecommendationRequest request) {
        log.debug("AI 추천 요청에 기본값 적용: topK={}, minSimilarity={}, usePriceFilter={}",
                request.topK(), request.minSimilarity(), request.usePriceFilter());

        // yml 설정값 우선, 없으면 안전한 기본값 사용
        final Integer finalTopK = request.topK() != null ? request.topK() : 
                (aiProperties.getDefaultTopK() != null ? aiProperties.getDefaultTopK() : 40);
        
        final Double finalMinSimilarity = request.minSimilarity() != null ? request.minSimilarity() : 
                (aiProperties.getDefaultMinSimilarity() != null ? aiProperties.getDefaultMinSimilarity() : 0.65);
        
        final Boolean finalUsePriceFilter = request.usePriceFilter() != null ? request.usePriceFilter() : 
                (aiProperties.getDefaultUsePriceFilter() != null ? aiProperties.getDefaultUsePriceFilter() : true);

        log.debug("적용된 기본값 - topK: {}, minSimilarity: {}, usePriceFilter: {}", 
                finalTopK, finalMinSimilarity, finalUsePriceFilter);

        return new GroupBuyRecommendationRequest(
                request.beautyProfile(),
                finalTopK,
                request.minPrice(),
                request.maxPrice(),
                request.additionalInfo(),
                request.interestCategories(),
                finalMinSimilarity,
                finalUsePriceFilter
        );
    }
}
