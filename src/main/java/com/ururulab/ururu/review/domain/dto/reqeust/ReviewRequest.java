package com.ururulab.ururu.review.domain.dto.reqeust;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.ururulab.ururu.global.common.entity.enumerated.Gender;
import com.ururulab.ururu.global.common.entity.enumerated.SkinType;
import com.ururulab.ururu.review.domain.entity.enumerated.AgeGroup;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewRequest(
		@NotNull
		Long productId,

		@NotNull
		Long productOptionId,

		@NotNull @Min(1) @Max(5)
		Integer rating,

		SkinType skinType,
		AgeGroup ageGroup,
		Gender gender,

		@NotBlank
		@Size(max = 1000)
		String content,

		@Size(max = 5)
		List<MultipartFile> imageFiles
) {
}
