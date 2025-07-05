package com.ururulab.ururu.member.dto.request;

import com.ururulab.ururu.global.validation.EnumValue;
import com.ururulab.ururu.member.dto.validation.MemberPreferenceValidationConstants;
import com.ururulab.ururu.member.dto.validation.MemberPreferenceValidationMessages;
import com.ururulab.ururu.member.domain.entity.enumerated.PurchaseFrequency;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MemberPreferenceRequest(
        @NotNull(message = MemberPreferenceValidationMessages.SELLER_ID_REQUIRED)
        Long sellerId,

        @NotNull(message = MemberPreferenceValidationMessages.PREFERENCE_LEVEL_REQUIRED)
        @Min(value = MemberPreferenceValidationConstants.PREFERENCE_LEVEL_MIN,
                message = MemberPreferenceValidationMessages.PREFERENCE_LEVEL_MIN)
        @Max(value = MemberPreferenceValidationConstants.PREFERENCE_LEVEL_MAX,
                message = MemberPreferenceValidationMessages.PREFERENCE_LEVEL_MAX)
        Integer preferenceLevel,

        @NotNull(message = MemberPreferenceValidationMessages.MONTHLY_BUDGET_REQUIRED)
        @Min(value = MemberPreferenceValidationConstants.MONTHLY_BUDGET_MIN,
                message = MemberPreferenceValidationMessages.MONTHLY_BUDGET_MIN)
        Integer monthlyBudget,

        @NotNull(message = MemberPreferenceValidationMessages.PURCHASE_FREQUENCY_REQUIRED)
        @EnumValue(enumClass = PurchaseFrequency.class,
                message = MemberPreferenceValidationMessages.PURCHASE_FREQUENCY_INVALID)
        String purchaseFrequency
) {
}
