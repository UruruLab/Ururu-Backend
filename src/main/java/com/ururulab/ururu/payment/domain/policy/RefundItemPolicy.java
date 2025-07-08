package com.ururulab.ururu.payment.domain.policy;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RefundItemPolicy {

    // 에러 메시지
    public static final String REFUND_REQUIRED = "환불 정보는 필수입니다.";
    public static final String ORDER_ITEM_REQUIRED = "주문 아이템 정보는 필수입니다.";
}