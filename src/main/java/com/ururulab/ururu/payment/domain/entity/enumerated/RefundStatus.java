package com.ururulab.ururu.payment.domain.entity.enumerated;

import com.ururulab.ururu.global.domain.entity.enumerated.EnumParser;

public enum RefundStatus {
    INITIATED,  // 환불 요청
    COMPLETED,  // 환불 완료
    FAILED;     // 환불 실패

    public static RefundStatus from(String value) {
        return EnumParser.fromString(RefundStatus.class, value, "RefundStatus");
    }
}