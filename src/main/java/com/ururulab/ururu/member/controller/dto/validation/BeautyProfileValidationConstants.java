package com.ururulab.ururu.member.controller.dto.validation;

import lombok.experimental.UtilityClass;

@UtilityClass
public class BeautyProfileValidationConstants {
    public static final int MAX_CONCERNS_COUNT = 3;
    public static final int CONCERN_ITEM_MAX_LENGTH = 50;

    public static final int ALLERGY_ITEM_MAX_LENGTH = 50;

    public static final int INTEREST_CATEGORY_ITEM_MAX_LENGTH = 50;

    public static final int MIN_PRICE_VALUE = 0;
    public static final int MAX_PRICE_VALUE = 0;

    public static final int ADDITIONAL_INFO_MAX_LENGTH = 1000;
}
