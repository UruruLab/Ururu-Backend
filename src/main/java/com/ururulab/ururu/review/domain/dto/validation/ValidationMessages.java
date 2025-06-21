package com.ururulab.ururu.review.domain.dto.validation;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ValidationMessages {

	public static final String PRODUCT_ID_REQUIRED = "상품 ID는 필수입니다.";
	public static final String PRODUCT_OPTION_ID_REQUIRED = "상품 옵션 ID는 필수입니다.";
	public static final String RATING_REQUIRED = "평점은 필수입니다.";
	public static final String RATING_MIN = "평점은 최소 1점입니다.";
	public static final String RATING_MAX = "평점은 최대 5점입니다.";
	public static final String SKIN_TYPE_INVALID = "skinType이 유효하지 않습니다.";
	public static final String AGE_GROUP_INVALID = "ageGroup이 유효하지 않습니다.";
	public static final String GENDER_INVALID = "gender가 유효하지 않습니다.";
	public static final String CONTENT_REQUIRED = "리뷰 내용은 필수입니다.";
	public static final String CONTENT_SIZE = "리뷰 내용은 최대 1000자까지 입력 가능합니다.";
	public static final String IMAGE_SIZE = "이미지는 최대 5개까지 첨부 가능합니다.";
	public static final String TAGS_MIN = "tags는 최소 1개 이상이어야 합니다.";
	public static final String TAG_ID_NOT_NULL = "tag id는 null일 수 없습니다.";

}
