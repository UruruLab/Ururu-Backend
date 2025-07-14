package com.ururulab.ururu.ai.service;

import com.ururulab.ururu.ai.config.AiRecommendationProperties;
import com.ururulab.ururu.ai.dto.GroupBuyRecommendationRequest;
import com.ururulab.ururu.global.domain.entity.enumerated.SkinTone;
import com.ururulab.ururu.global.domain.entity.enumerated.SkinType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Spring Boot 요청을 AI 서비스 요청 형식으로 변환하는 서비스.
 * 
 * 책임: 요청 데이터 변환 및 매핑 로직
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiRequestMappingService {

    private final AiRecommendationProperties aiProperties;

    /**
     * GroupBuyRecommendationRequest를 AI 서비스 요청 형식으로 변환.
     *
     * @param request Spring Boot 추천 요청
     * @return AI 서비스 요청 본문
     */
    public Map<String, Object> mapToAiRequest(final GroupBuyRecommendationRequest request) {
        log.debug("AI 서비스 요청 변환 시작 - 피부타입: {}", request.beautyProfile().skinType());

        final Map<String, Object> beautyProfileMap = buildBeautyProfileMap(request);
        final List<String> includeCategories = getIncludeCategories(request);

        return Map.of(
                "beauty_profile", beautyProfileMap,
                "top_k", getTopK(request),
                "include_categories", includeCategories,
                "min_similarity", getMinSimilarity(request),
                "use_price_filter", getUsePriceFilter(request)
        );
    }

    private Map<String, Object> buildBeautyProfileMap(final GroupBuyRecommendationRequest request) {
        final var beautyProfile = request.beautyProfile();
        
        return Map.of(
                "skin_type", convertSkinTypeToKorean(beautyProfile.skinType()),
                "skin_tone", convertSkinToneToKorean(beautyProfile.skinTone()),
                "concerns", beautyProfile.concerns() != null ? beautyProfile.concerns() : List.of(),
                "has_allergy", beautyProfile.hasAllergy() != null ? beautyProfile.hasAllergy() : false,
                "allergies", beautyProfile.allergies() != null ? beautyProfile.allergies() : List.of(),
                "interest_categories", convertCategoriesToKorean(beautyProfile.interestCategories()),
                "min_price", getMinPrice(request),
                "max_price", getMaxPrice(request),
                "additional_info", getAdditionalInfo(request)
        );
    }

    private String convertSkinTypeToKorean(final String skinType) {
        if (skinType == null || skinType.isEmpty()) {
            return "지성";
        }
        
        return switch (skinType.toUpperCase()) {
            case "OILY" -> "지성";
            case "DRY" -> "건성";
            case "SENSITIVE" -> "민감성";
            case "COMBINATION" -> "복합성";
            case "VERY_DRY" -> "악건성";
            case "TROUBLE" -> "트러블성";
            case "NEUTRAL" -> "중성";
            default -> "지성";
        };
    }

    private String convertSkinToneToKorean(final String skinTone) {
        if (skinTone == null || skinTone.isEmpty()) {
            return "웜톤";
        }
        
        return switch (skinTone.toUpperCase()) {
            case "WARM" -> "웜톤";
            case "COOL" -> "쿨톤";
            case "NEUTRAL" -> "뉴트럴톤";
            case "SPRING_WARM" -> "봄웜톤";
            case "SUMMER_COOL" -> "여름쿨톤";
            case "AUTUMN_WARM" -> "가을웜톤";
            case "WINTER_COOL" -> "겨울쿨톤";
            default -> "웜톤";
        };
    }

    private List<String> convertCategoriesToKorean(final List<String> categories) {
        if (categories == null || categories.isEmpty()) {
            return List.of("스킨케어");
        }
        
        return categories.stream()
                .map(this::convertCategoryToKorean)
                .toList();
    }

    private String convertCategoryToKorean(final String category) {
        if (category == null || category.isEmpty()) {
            return "스킨케어";
        }
        
        return switch (category.toLowerCase()) {
            case "skincare", "스킨케어" -> "스킨케어";
            case "makeup", "메이크업" -> "메이크업";
            case "cleansing", "클렌징" -> "클렌징";
            case "mask", "마스크팩" -> "마스크팩";
            case "suncare", "선케어" -> "선케어";
            case "perfume", "향수" -> "향수";
            case "haircare", "헤어케어" -> "헤어케어";
            case "bodycare", "바디케어" -> "바디케어";
            default -> "스킨케어";
        };
    }

    private Integer getTopK(final GroupBuyRecommendationRequest request) {
        return request.topK(); // 이미 RequestProcessor에서 처리됨
    }

    private List<String> getIncludeCategories(final GroupBuyRecommendationRequest request) {
        return convertCategoriesToKorean(request.interestCategories());
    }

    private Double getMinSimilarity(final GroupBuyRecommendationRequest request) {
        return request.minSimilarity(); // 이미 RequestProcessor에서 처리됨
    }

    private Boolean getUsePriceFilter(final GroupBuyRecommendationRequest request) {
        return request.usePriceFilter(); // 이미 RequestProcessor에서 처리됨
    }

    private Integer getMinPrice(final GroupBuyRecommendationRequest request) {
        return request.minPrice() != null ? request.minPrice() : 10;
    }

    private Integer getMaxPrice(final GroupBuyRecommendationRequest request) {
        return request.maxPrice() != null ? request.maxPrice() : 10000000;
    }

    private String getAdditionalInfo(final GroupBuyRecommendationRequest request) {
        return request.additionalInfo() != null ? request.additionalInfo() : "";
    }
}
