package com.ururulab.ururu.groupBuy.domain.dto.validation;

import lombok.experimental.UtilityClass;

@UtilityClass
public class GroupBuyValidationConstants {
    public static final int GROUP_BUY_TITLE_MAX = 100;

    public static final int GROUP_BUY_DESCRIPTION_MAX = 2000;

    // 수량 제한
    public static final int LIMIT_QUANTITY_MIN = 1;
    public static final int LIMIT_QUANTITY_MAX = 999999;

    // 이미지 관련
    public static final int MAX_GROUP_BUY_THUMBNAIL_IMAGES = 3;
    public static final int MAX_GROUP_BUY_DETAIL_IMAGES = 10;
}
