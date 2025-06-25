package com.ururulab.ururu.order.domain.policy;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CartItemPolicy {
    // 정책 상수
    public static final int MIN_QUANTITY = 1;

    // 에러 메시지
    public static final String GROUPBUY_OPTION_REQUIRED = "공동구매 상품 옵션은 필수입니다.";
    public static final String QUANTITY_MIN = "수량은 1 이상이어야 합니다.";
    public static final String INCREASE_AMOUNT_MIN = "증가량은 1 이상이어야 합니다.";
    public static final String DECREASE_AMOUNT_MIN = "감소량은 1 이상이어야 합니다.";
}