package com.ururulab.ururu.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record GroupBuyRecommendationRequest(
    @Valid @NotNull(message = "뷰티 프로필은 필수입니다")
    BeautyProfile beautyProfile,
    
    @Min(value = 1, message = "추천 개수는 1개 이상이어야 합니다")
    @Max(value = 100, message = "추천 개수는 100개 이하여야 합니다")
    Integer topK,
    
    String ageGroup,
    String priceRange,
    
    @Min(value = 10, message = "최소 가격은 10원 이상이어야 합니다")
    Integer minPrice,
    
    @Max(value = 10000000, message = "최대 가격은 1000만원 이하여야 합니다")
    Integer maxPrice,
    
    String additionalInfo,
    
    List<String> interestCategories,
    
    @Min(value = 0, message = "유사도는 0 이상이어야 합니다")
    @Max(value = 1, message = "유사도는 1 이하여야 합니다")
    Double minSimilarity,
    
    Boolean usePriceFilter
) {
    public record BeautyProfile(
        @NotNull(message = "피부 타입은 필수입니다")
        String skinType,
        
        String skinTone,
        
        List<String> concerns,
        
        Boolean hasAllergy,
        
        List<String> allergies,
        
        List<String> interestCategories
    ) {}
    
    public GroupBuyRecommendationRequest withMemberId(final Long memberId) {
        // memberId는 인증을 통해 처리되므로 DTO에서 제거
        return this;
    }
    
    public static GroupBuyRecommendationRequest ofBeautyProfile(
            final String skinType,
            final String skinTone,
            final List<String> concerns,
            final Boolean hasAllergy,
            final List<String> allergies,
            final List<String> interestCategories,
            final Integer topK,
            final String ageGroup,
            final String priceRange,
            final Integer minPrice,
            final Integer maxPrice,
            final String additionalInfo,
            final Double minSimilarity,
            final Boolean usePriceFilter
    ) {
        final BeautyProfile beautyProfile = new BeautyProfile(
                skinType,
                skinTone,
                concerns,
                hasAllergy,
                allergies,
                interestCategories
        );
        
        return new GroupBuyRecommendationRequest(
                beautyProfile,
                topK != null ? topK : 40,
                ageGroup,
                priceRange,
                minPrice,
                maxPrice,
                additionalInfo,
                interestCategories,
                minSimilarity != null ? minSimilarity : 0.3,
                usePriceFilter != null ? usePriceFilter : true
        );
    }
}
