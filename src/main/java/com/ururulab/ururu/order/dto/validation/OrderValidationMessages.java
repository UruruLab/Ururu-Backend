package com.ururulab.ururu.order.dto.validation;

import lombok.experimental.UtilityClass;

@UtilityClass
public class OrderValidationMessages {

    // 주문 아이템 관련
    public static final String GROUPBUY_OPTION_ID_REQUIRED = "공구 옵션 ID는 필수입니다.";
    public static final String QUANTITY_REQUIRED = "수량은 필수입니다.";
    public static final String QUANTITY_MIN = "수량은 최소 1개입니다.";
    public static final String QUANTITY_MAX = "수량은 최대 999개까지 가능합니다.";

    // 공구 주문 관련
    public static final String ORDER_ITEMS_REQUIRED = "주문 아이템은 최소 1개 이상이어야 합니다.";
    public static final String ORDER_ITEMS_MAX = "한 번에 최대 50개 옵션까지 주문 가능합니다.";

    // 장바구니 주문 관련
    public static final String CART_ITEM_IDS_REQUIRED = "장바구니 아이템은 최소 1개 이상 선택해야 합니다.";
    public static final String CART_ITEM_IDS_MAX = "한 번에 최대 50개 아이템까지 주문 가능합니다.";
}