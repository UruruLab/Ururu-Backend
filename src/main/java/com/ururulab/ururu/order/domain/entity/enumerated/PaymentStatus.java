package com.ururulab.ururu.order.domain.entity.enumerated;

import com.ururulab.ururu.global.common.entity.enumerated.EnumParser;

public enum PaymentStatus {
    PENDING,    // 결제 대기
    PAID,       // 결제 완료
    REFUNDED,   // 환불 완료
    FAILED;     // 결제 실패

    public static PaymentStatus from(String value) {
        return EnumParser.fromString(PaymentStatus.class, value, "PaymentStatus");
    }
}