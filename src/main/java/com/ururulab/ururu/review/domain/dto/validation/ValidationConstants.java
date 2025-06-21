package com.ururulab.ururu.review.domain.dto.validation;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ValidationConstants {

	public static final int RATING_MIN = 1;
	public static final int RATING_MAX = 5;

	public static final int CONTENT_MAX_LENGTH = 1000;

	public static final int IMAGE_MAX_COUNT = 5;

	public static final int TAGS_MIN_COUNT = 1;
}
