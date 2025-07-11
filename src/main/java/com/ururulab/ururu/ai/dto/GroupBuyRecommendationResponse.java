package com.ururulab.ururu.ai.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record GroupBuyRecommendationResponse(
    List<RecommendedGroupBuy> recommendedGroupBuys,
    String recommendationReason,
    LocalDateTime recommendedAt,
    String cacheSource
) {
    
    public record RecommendedGroupBuy(
        Long groupBuyId,
        String groupBuyTitle,
        Long productId,
        String productName,
        BigDecimal originalPrice,
        BigDecimal discountedPrice,
        String imageUrl,
        Double similarityScore,
        String category,
        List<String> keyIngredients,
        String matchReason,
        Integer currentParticipants,
        Integer minParticipants,
        LocalDateTime endDate
    ) {
        public static RecommendedGroupBuy of(
                final Long groupBuyId,
                final String groupBuyTitle,
                final Long productId,
                final String productName,
                final BigDecimal originalPrice,
                final BigDecimal discountedPrice,
                final String imageUrl,
                final Double similarityScore,
                final String category,
                final List<String> keyIngredients,
                final String matchReason,
                final Integer currentParticipants,
                final Integer minParticipants,
                final LocalDateTime endDate
        ) {
            return new RecommendedGroupBuy(
                    groupBuyId, groupBuyTitle, productId, productName,
                    originalPrice, discountedPrice, imageUrl, similarityScore,
                    category, keyIngredients, matchReason,
                    currentParticipants, minParticipants, endDate
            );
        }
    }
    
    public static GroupBuyRecommendationResponse of(
            final List<RecommendedGroupBuy> recommendedGroupBuys,
            final String recommendationReason,
            final LocalDateTime recommendedAt,
            final String cacheSource
    ) {
        return new GroupBuyRecommendationResponse(
                recommendedGroupBuys,
                recommendationReason,
                recommendedAt,
                cacheSource
        );
    }
}
