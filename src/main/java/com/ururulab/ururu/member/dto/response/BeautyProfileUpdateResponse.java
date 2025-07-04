package com.ururulab.ururu.member.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ururulab.ururu.global.domain.entity.enumerated.SkinTone;
import com.ururulab.ururu.global.domain.entity.enumerated.SkinType;
import com.ururulab.ururu.member.domain.entity.BeautyProfile;

import java.time.Instant;
import java.util.List;

public record BeautyProfileUpdateResponse(
        Long id,
        @JsonProperty("member_id") Long memberId,
        @JsonProperty("skin_type") SkinType skinType,
        @JsonProperty("skin_tone") SkinTone skinTone,
        List<String> concerns,
        @JsonProperty("has_allergy") Boolean hasAllergy,
        List<String> allergies,
        @JsonProperty("interest_categories") List<String> interestCategories,
        @JsonProperty("min_price") Integer minPrice,
        @JsonProperty("max_price") Integer maxPrice,
        @JsonProperty("additional_info") String additionalInfo,
        @JsonProperty("updated_at") Instant updatedAt
) {
    public static BeautyProfileUpdateResponse from(final BeautyProfile beautyProfile) {
        return new BeautyProfileUpdateResponse(
                beautyProfile.getId(),
                beautyProfile.getMember().getId(),
                beautyProfile.getSkinType(),
                beautyProfile.getSkinTone(),
                List.copyOf(beautyProfile.getConcerns()),
                beautyProfile.getHasAllergy(),
                beautyProfile.getAllergies() != null ? List.copyOf(beautyProfile.getAllergies()) : List.of(),
                List.copyOf(beautyProfile.getInterestCategories()),
                beautyProfile.getMinPrice(),
                beautyProfile.getMaxPrice(),
                beautyProfile.getAdditionalInfo(),
                beautyProfile.getUpdatedAt()
        );
    }
}
