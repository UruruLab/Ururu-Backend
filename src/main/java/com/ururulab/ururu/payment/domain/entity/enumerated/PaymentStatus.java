package com.ururulab.ururu.payment.domain.entity.enumerated;

import com.ururulab.ururu.global.domain.entity.enumerated.EnumParser;

public enum PaymentStatus {
    PENDING,    // 결제 대기
    PAID,       // 결제 완료
    FAILED,     // 결제 실패
    PARTIAL_REFUNDED, // 부분 환불 완료
    REFUNDED;   // 환불 완료

    public static PaymentStatus from(String value) {
        return EnumParser.fromString(PaymentStatus.class, value, "PaymentStatus");
    }
}