package com.ururulab.ururu.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record GroupBuyRecommendationRequest(
    @NotNull(message = "회원 ID는 필수입니다")
    Long memberId,
    
    @NotBlank(message = "피부 타입은 필수입니다")  
    String skinType,
    
    @Size(min = 1, message = "최소 1개의 피부 고민을 선택해주세요")
    List<String> skinConcerns,
    
    List<String> preferredCategories,
    String ageGroup,
    String priceRange,
    
    @Min(value = 1, message = "추천 개수는 1개 이상이어야 합니다")
    @Max(value = 100, message = "추천 개수는 100개 이하여야 합니다")
    Integer topK
) {
    public static GroupBuyRecommendationRequest ofBeautyProfile(
            final Long memberId,
            final String skinType,
            final List<String> skinConcerns,
            final List<String> preferredCategories,
            final String ageGroup,
            final String priceRange,
            final Integer topK
    ) {
        return new GroupBuyRecommendationRequest(
                memberId,
                skinType,
                skinConcerns,
                preferredCategories,
                ageGroup,
                priceRange,
                topK != null ? topK : 40
        );
    }
}
