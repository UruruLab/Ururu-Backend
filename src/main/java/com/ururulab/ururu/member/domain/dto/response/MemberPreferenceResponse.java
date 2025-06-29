package com.ururulab.ururu.member.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ururulab.ururu.member.domain.entity.MemberPreference;
import com.ururulab.ururu.member.domain.entity.enumerated.PurchaseFrequency;

import java.time.ZonedDateTime;

public record MemberPreferenceResponse(
        Long id,
        @JsonProperty("seller_id") Long sellerId,
        @JsonProperty("preference_level") Integer preferenceLevel,
        @JsonProperty("monthly_budget") Integer monthlyBudget,
        @JsonProperty("purchase_frequency") PurchaseFrequency purchaseFrequency,
        @JsonProperty("created_at") ZonedDateTime createdAt,
        @JsonProperty("updated_at") ZonedDateTime updatedAt
) {
    public static MemberPreferenceResponse from(final MemberPreference memberPreference) {
        return new MemberPreferenceResponse(
                memberPreference.getId(),
                memberPreference.getSellerId(),
                memberPreference.getPreferenceLevel(),
                memberPreference.getMonthlyBudget(),
                memberPreference.getPurchaseFrequency(),
                memberPreference.getCreatedAt(),
                memberPreference.getUpdatedAt()
        );
    }
}
