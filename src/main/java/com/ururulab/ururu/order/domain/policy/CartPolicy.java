package com.ururulab.ururu.order.domain.policy;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CartPolicy {

    // 에러 메시지
    public static final String MEMBER_REQUIRED = "회원 정보는 필수입니다.";
    public static final String CART_ITEM_REQUIRED = "장바구니 아이템은 필수입니다.";
    public static final String CART_ITEM_ID_REQUIRED = "장바구니 아이템 ID는 필수입니다.";
}