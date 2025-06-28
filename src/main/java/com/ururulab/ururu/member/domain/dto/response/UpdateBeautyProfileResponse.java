package com.ururulab.ururu.member.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ururulab.ururu.global.domain.entity.enumerated.SkinType;
import com.ururulab.ururu.member.domain.entity.BeautyProfile;

import java.time.ZonedDateTime;
import java.util.List;

public record UpdateBeautyProfileResponse(
        Long id,
        @JsonProperty("skin_type") SkinType skinType,
        List<String> concerns,
        @JsonProperty("has_allergy") Boolean hasAllergy,
        List<String> allergies,
        @JsonProperty("interest_categories") List<String> interestCategories,
        @JsonProperty("min_price") Integer minPrice,
        @JsonProperty("max_price") Integer maxPrice,
        @JsonProperty("additional_info") String additionalInfo,
        @JsonProperty("updated_at") ZonedDateTime updatedAt
) {
    public static UpdateBeautyProfileResponse from(final BeautyProfile beautyProfile) {
        return new UpdateBeautyProfileResponse(
                beautyProfile.getId(),
                beautyProfile.getSkinType(),
                beautyProfile.getConcerns(),
                beautyProfile.getHasAllergy(),
                beautyProfile.getAllergies(),
                beautyProfile.getInterestCategories(),
                beautyProfile.getMinPrice(),
                beautyProfile.getMaxPrice(),
                beautyProfile.getAdditionalInfo(),
                beautyProfile.getUpdatedAt()
        );
    }
}
