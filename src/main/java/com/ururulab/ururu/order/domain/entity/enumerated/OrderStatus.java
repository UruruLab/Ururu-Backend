package com.ururulab.ururu.order.domain.entity.enumerated;

import com.ururulab.ururu.global.domain.entity.enumerated.EnumParser;

public enum OrderStatus {
    PENDING,    // 주문서 생성 (재고 예약, 결제 대기)
    ORDERED,    // 주문 완료 (결제 완료, 재고 확정)
    CANCELLED,  // 주문 취소
    REFUNDED;   // 환불 완료

    public static OrderStatus from(String value) {
        return EnumParser.fromString(OrderStatus.class, value, "OrderStatus");
    }
}