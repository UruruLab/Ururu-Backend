package com.ururulab.ururu.member.controller.dto.validation;

import lombok.experimental.UtilityClass;

@UtilityClass
public class BeautyProfileValidationMessages {
    public static final String SKIN_TYPE_REQUIRED = "피부 타입은 필수입니다.";
    public static final String SKIN_TYPE_INVALID = "유효하지 않은 피부 타입입니다.";

    public static final String SKIN_TONE_INVALID = "유효하지 않은 피부 톤입니다.";

    public static final String SKIN_CONCERNS_SIZE = "피부 고민은 최대 " +
            BeautyProfileValidationConstants.MAX_CONCERNS_COUNT + "개까지 선택 가능합니다.";
    public static final String SKIN_CONCERN_ITEM_SIZE = "피부 고민 항목은 " +
            BeautyProfileValidationConstants.CONCERN_ITEM_MAX_LENGTH + "자 이하여야 합니다.";

    public static final String HAS_ALLERGY_REQUIRED = "알러지 여부는 필수입니다.";
    public static final String ALLERGY_ITEM_SIZE = "알러지 항목은 " +
            BeautyProfileValidationConstants.ALLERGY_ITEM_MAX_LENGTH + "자 이하여야 합니다.";
    public static final String ALLERGY_INCONSISTENCY = "알러지가 있다고 선택하셨습니다. 알러지 목록을 입력해주세요.";
    public static final String NO_ALLERGY_INCONSISTENCY = "알러지가 없다고 선택하셨습니다. 알러지 목록을 비워주세요.";

    public static final String INTEREST_CATEGORIES_REQUIRED = "관심 카테고리는 필수입니다.";
    public static final String INTEREST_CATEGORY_ITEM_SIZE = "관심 카테고리 항목은 " +
            BeautyProfileValidationConstants.INTEREST_CATEGORY_ITEM_MAX_LENGTH + "자 이하여야 합니다.";

    public static final String MIN_PRICE_INVALID = "최소 가격은 " +
            BeautyProfileValidationConstants.MIN_PRICE_VALUE + "원 이상이어야 합니다.";
    public static final String MAX_PRICE_INVALID = "최대 가격은 " +
            BeautyProfileValidationConstants.MAX_PRICE_VALUE + "원 이상이어야 합니다.";
    public static final String PRICE_RANGE_INVALID = "최소 가격은 최대 가격보다 작거나 같아야 합니다.";

    public static final String ADDITIONAL_INFO_SIZE = "추가 정보는 " +
            BeautyProfileValidationConstants.ADDITIONAL_INFO_MAX_LENGTH + "자 이하여야 합니다.";

}
