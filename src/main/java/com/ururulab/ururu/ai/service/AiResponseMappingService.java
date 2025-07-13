package com.ururulab.ururu.ai.service;

import com.ururulab.ururu.ai.dto.GroupBuyRecommendationResponse.RecommendedGroupBuy;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI 서비스 응답을 Spring Boot 응답 형식으로 변환하는 서비스.
 * 
 * 책임: 응답 데이터 변환 및 매핑 로직
 */
@Service
@Slf4j
public class AiResponseMappingService {

    /**
     * AI 서비스 응답을 RecommendedGroupBuy 리스트로 변환.
     *
     * @param aiResponse AI 서비스 원본 응답
     * @return 변환된 추천 상품 리스트
     * @throws BusinessException 응답 변환 실패 시
     */
    @SuppressWarnings("unchecked")
    public List<RecommendedGroupBuy> mapToRecommendedGroupBuys(final Map<String, Object> aiResponse) {
        try {
            log.debug("AI 응답 변환 시작");

            final List<Map<String, Object>> recommendations = 
                (List<Map<String, Object>>) aiResponse.get("recommendations");
            
            if (recommendations == null || recommendations.isEmpty()) {
                log.warn("AI 응답에서 recommendations 필드가 없거나 비어있음");
                return List.of();
            }

            final List<RecommendedGroupBuy> result = recommendations.stream()
                    .map(this::mapSingleRecommendation)
                    .filter(java.util.Objects::nonNull)
                    .toList();

            log.debug("AI 응답 변환 완료 - 변환된 추천 수: {}", result.size());
            return result;
                    
        } catch (final ClassCastException e) {
            log.error("AI 응답 구조 변환 실패", e);
            throw new BusinessException(ErrorCode.AI_INVALID_RESPONSE_FORMAT);
        }
    }

    @SuppressWarnings("unchecked")
    private RecommendedGroupBuy mapSingleRecommendation(final Map<String, Object> aiRecommendation) {
        try {
            final Map<String, Object> productInfo = 
                (Map<String, Object>) aiRecommendation.get("product");
            
            if (productInfo == null) {
                log.warn("AI 추천 결과에 product 정보가 없음");
                return null;
            }

            // 필수 필드 추출
            final Long productId = extractProductId(productInfo);
            if (productId == null) {
                log.warn("AI 추천 결과에 상품 ID가 없음");
                return null;
            }

            final String productName = extractProductName(productInfo);
            final BigDecimal originalPrice = extractPrice(productInfo);
            final BigDecimal discountedPrice = originalPrice; // 임시로 동일하게 설정
            final Double similarity = extractSimilarity(aiRecommendation);
            final String recommendReason = extractRecommendReason(aiRecommendation);

            return RecommendedGroupBuy.of(
                    productId, // groupBuyId - 임시로 productId 사용
                    "공동구매 - " + productName, // groupBuyTitle
                    productId, // productId
                    productName, // productName
                    originalPrice, // originalPrice
                    discountedPrice, // discountedPrice
                    "", // imageUrl - 기본값
                    similarity, // similarityScore
                    "AI추천", // category - 기본값
                    List.of(), // keyIngredients - 기본값
                    recommendReason, // matchReason
                    0, // currentParticipants - 기본값
                    10, // minParticipants - 기본값
                    LocalDateTime.now().plusDays(7) // endDate - 기본값
            );
                    
        } catch (final Exception e) {
            log.error("개별 추천 결과 변환 실패", e);
            return null;
        }
    }

    private Long extractProductId(final Map<String, Object> productInfo) {
        final Number productIdNum = (Number) productInfo.get("id");
        return productIdNum != null ? productIdNum.longValue() : null;
    }

    private String extractProductName(final Map<String, Object> productInfo) {
        return (String) productInfo.getOrDefault("name", "상품명 없음");
    }

    private BigDecimal extractPrice(final Map<String, Object> productInfo) {
        final Number priceNum = (Number) productInfo.get("price");
        return priceNum != null ? BigDecimal.valueOf(priceNum.doubleValue()) : BigDecimal.ZERO;
    }

    private Double extractSimilarity(final Map<String, Object> aiRecommendation) {
        final Number similarityNum = (Number) aiRecommendation.get("similarity_score");
        return similarityNum != null ? similarityNum.doubleValue() : 0.0;
    }

    private String extractRecommendReason(final Map<String, Object> aiRecommendation) {
        return (String) aiRecommendation.getOrDefault("recommendation_reason", "AI 분석 기반 추천");
    }
}
