package com.ururulab.ururu.groupBuy.controller.dto.validation;

import lombok.experimental.UtilityClass;

@UtilityClass
public class GroupBuyValidationConstants {
    public static final int GROUP_BUY_TITLE_MAX = 100;

    public static final int GROUP_BUY_DESCRIPTION_MAX = 2000;

    // 수량 제한
    public static final int LIMIT_QUANTITY_MIN = 1;
    public static final int LIMIT_QUANTITY_MAX = 999;

    // 할인 제한
    public static final int LIMIT_RATE_MIN = 0;
    public static final int LIMIT_RATE_MAX = 100;

    // 이미지 관련
    public static final int MAX_GROUP_BUY_THUMBNAIL_IMAGES = 3;
    public static final int MAX_GROUP_BUY_DETAIL_IMAGES = 10;

    // 할인율 등록
    public static final String MIN_QUANTITY_REQUIRED_MSG = "최소 달성 수량은 필수입니다.";
    public static final String MIN_QUANTITY_MIN = "최소 달성 수량은 1 이상이어야 합니다.";

    public static final String DISCOUNT_RATE_REQUIRED_MSG = "할인율은 필수입니다.";
    public static final String DISCOUNT_RATE_MIN = "할인율은 0% 이상이어야 합니다.";
    public static final String DISCOUNT_RATE_MAX = "할인율은 100% 이하여야 합니다.";
}
