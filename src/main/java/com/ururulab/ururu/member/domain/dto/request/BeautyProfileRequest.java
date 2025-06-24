package com.ururulab.ururu.member.domain.dto.request;

import com.ururulab.ururu.global.common.entity.enumerated.SkinType;
import com.ururulab.ururu.global.validation.EnumValue;
import com.ururulab.ururu.member.domain.dto.validation.BeautyProfileValidationConstants;
import com.ururulab.ururu.member.domain.dto.validation.BeautyProfileValidationMessages;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BeautyProfileRequest(
        @NotNull(message = BeautyProfileValidationMessages.SKIN_TYPE_REQUIRED)
        @EnumValue(enumClass = SkinType.class, message = BeautyProfileValidationMessages.SKIN_TYPE_INVALID)
        SkinType skinType,

        @Size(max = BeautyProfileValidationConstants.MAX_CONCERNS_COUNT,
                message = BeautyProfileValidationMessages.SKIN_CONCERNS_SIZE)
        List<@Size(max = BeautyProfileValidationConstants.CONCERN_ITEM_MAX_LENGTH,
                message = BeautyProfileValidationMessages.SKIN_CONCERN_ITEM_SIZE)
                String> concerns,

        @NotNull(message = BeautyProfileValidationMessages.HAS_ALLERGY_REQUIRED)
        Boolean hasAllergy,

        List<@Size(max = BeautyProfileValidationConstants.ALLERGY_ITEM_MAX_LENGTH,
                message = BeautyProfileValidationMessages.ALLERGY_ITEM_SIZE)
                String> allergies,

        @NotNull(message = BeautyProfileValidationMessages.INTEREST_CATEGORIES_REQUIRED)
        List<@Size(max = BeautyProfileValidationConstants.INTEREST_CATEGORY_ITEM_MAX_LENGTH,
                message = BeautyProfileValidationMessages.INTEREST_CATEGORY_ITEM_SIZE)
                String> interestCategories,

        @Min(value = BeautyProfileValidationConstants.MIN_PRICE_VALUE,
                message = BeautyProfileValidationMessages.MIN_PRICE_INVALID)
        int minPrice,

        @Min(value = BeautyProfileValidationConstants.MAX_PRICE_VALUE,
                message = BeautyProfileValidationMessages.MAX_PRICE_INVALID)
        int maxPrice,

        @Size(max = BeautyProfileValidationConstants.ADDITIONAL_INFO_MAX_LENGTH,
                message = BeautyProfileValidationMessages.ADDITIONAL_INFO_SIZE)
        String additionalInfo
) {
}
