package com.ururulab.ururu.order.domain.entity.enumerated;

import com.ururulab.ururu.global.domain.entity.enumerated.EnumParser;

public enum OrderStatus {
    ORDERED,    // 주문 완료
    CANCELLED,  // 주문 취소
    REFUNDED;   // 환불 완료

    public static OrderStatus from(String value) {
        return EnumParser.fromString(OrderStatus.class, value, "OrderStatus");
    }
}