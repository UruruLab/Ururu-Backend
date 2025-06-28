package com.ururulab.ururu.payment.domain.entity.enumerated;

import com.ururulab.ururu.global.domain.entity.enumerated.EnumParser;

public enum PointSource {
    GROUPBUY,   // 공동구매
    REVIEW,     // 리뷰 작성
    INVITE,     // 친구 초대
    ADMIN,      // 관리자 지급
    REFUND;     // 환불로 인한 적립

    public static PointSource from(String value) {
        return EnumParser.fromString(PointSource.class, value, "PointSource");
    }
}