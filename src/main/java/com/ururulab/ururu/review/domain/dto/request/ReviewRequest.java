package com.ururulab.ururu.review.domain.dto.request;

import java.util.List;

import com.ururulab.ururu.global.domain.entity.enumerated.Gender;
import com.ururulab.ururu.global.domain.entity.enumerated.SkinType;
import com.ururulab.ururu.global.validation.EnumValue;
import com.ururulab.ururu.review.domain.dto.validation.ValidationConstants;
import com.ururulab.ururu.review.domain.dto.validation.ValidationMessages;
import com.ururulab.ururu.review.domain.entity.enumerated.AgeGroup;
import com.ururulab.ururu.review.domain.policy.ReviewPolicy;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewRequest(
		@NotNull(message = ValidationMessages.PRODUCT_ID_REQUIRED)
		Long productId,

		@NotNull(message = ValidationMessages.PRODUCT_OPTION_ID_REQUIRED)
		Long productOptionId,

		@NotNull(message = ValidationMessages.RATING_REQUIRED)
		@Min(value = ReviewPolicy.RATING_MIN, message = ValidationMessages.RATING_MIN)
		@Max(value = ReviewPolicy.RATING_MAX, message = ValidationMessages.RATING_MAX)
		Integer rating,

		@EnumValue(enumClass = SkinType.class, message = ValidationMessages.SKIN_TYPE_INVALID)
		String skinType,

		@EnumValue(enumClass = AgeGroup.class, message = ValidationMessages.AGE_GROUP_INVALID)
		String ageGroup,

		@EnumValue(enumClass = Gender.class, message = ValidationMessages.GENDER_INVALID)
		String gender,

		@NotBlank(message = ValidationMessages.CONTENT_REQUIRED)
		@Size(max = ReviewPolicy.CONTENT_MAX_LENGTH, message = ValidationMessages.CONTENT_SIZE)
		String content,

		@Size(min = ValidationConstants.TAGS_MIN_COUNT, message = ValidationMessages.TAGS_MIN)
		List<@NotNull(message = ValidationMessages.TAG_ID_NOT_NULL) Long> tags
) {
}
