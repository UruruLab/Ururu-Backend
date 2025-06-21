package com.ururulab.ururu.review.domain.dto.request;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.ururulab.ururu.review.domain.dto.validation.ValidationConstants;
import com.ururulab.ururu.review.domain.dto.validation.ValidationMessages;
import com.ururulab.ururu.review.domain.dto.validation.ValidationPatterns;
import com.ururulab.ururu.review.domain.policy.ReviewImagePolicy;
import com.ururulab.ururu.review.domain.policy.ReviewPolicy;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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

		@Pattern(regexp = ValidationPatterns.SKIN_TYPE_PATTERN,
				flags = Pattern.Flag.CASE_INSENSITIVE,
				message = ValidationMessages.SKIN_TYPE_INVALID)
		String skinType,

		@Pattern(regexp = ValidationPatterns.AGE_GROUP_PATTERN,
				flags = Pattern.Flag.CASE_INSENSITIVE,
				message = ValidationMessages.AGE_GROUP_INVALID)
		String ageGroup,

		@Pattern(regexp = ValidationPatterns.GENDER_PATTERN,
				flags = Pattern.Flag.CASE_INSENSITIVE,
				message = ValidationMessages.GENDER_INVALID)
		String gender,

		@NotBlank(message = ValidationMessages.CONTENT_REQUIRED)
		@Size(max = ReviewPolicy.CONTENT_MAX_LENGTH, message = ValidationMessages.CONTENT_SIZE)
		String content,

		@Size(max = ReviewImagePolicy.MAX_IMAGE_COUNT, message = ValidationMessages.IMAGE_SIZE)
		List<MultipartFile> imageFiles,

		@Size(min = ValidationConstants.TAGS_MIN_COUNT, message = ValidationMessages.TAGS_MIN)
		List<@NotNull(message = ValidationMessages.TAG_ID_NOT_NULL) Long> tags
) {
}
