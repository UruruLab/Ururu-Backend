package com.ururulab.ururu.payment.domain.entity.enumerated;

import com.ururulab.ururu.global.domain.entity.enumerated.EnumParser;

public enum RefundType {
    GROUPBUY_FAILED,       // 공구 실패
    CHANGE_OF_MIND,        // 단순 변심
    DEFECTIVE_PRODUCT,     // 불량품
    DELIVERY_ISSUE,        // 배송 문제
    OTHER;                 // 기타


    public static RefundType from(String value) {
        return EnumParser.fromString(RefundType.class, value, "RefundType");
    }
}