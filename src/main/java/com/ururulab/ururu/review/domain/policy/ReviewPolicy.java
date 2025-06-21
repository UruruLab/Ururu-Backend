package com.ururulab.ururu.review.domain.policy;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ReviewPolicy {

	public static final int RATING_MIN = 1;
	public static final int RATING_MAX = 5;
	public static final int CONTENT_MAX_LENGTH = 1000;
}
