package com.ururulab.ururu.order.dto.validation;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CartValidationMessages {

    // 장바구니 추가 관련
    public static final String GROUPBUY_OPTION_ID_REQUIRED = "공구 옵션 ID는 필수입니다.";
    public static final String QUANTITY_REQUIRED = "수량은 필수입니다.";
    public static final String QUANTITY_MIN = "수량은 최소 1개입니다.";
    public static final String QUANTITY_MAX = "수량은 최대 999개까지 가능합니다.";

    // 수량 변경 관련
    public static final String QUANTITY_CHANGE_REQUIRED = "수량 변화량은 필수입니다.";
    public static final String QUANTITY_CHANGE_MIN = "수량 변화량은 최소 -999개입니다.";
    public static final String QUANTITY_CHANGE_MAX = "수량 변화량은 최대 +999개입니다.";
}