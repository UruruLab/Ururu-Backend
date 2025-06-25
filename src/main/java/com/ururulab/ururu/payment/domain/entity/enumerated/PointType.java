package com.ururulab.ururu.payment.domain.entity.enumerated;

import com.ururulab.ururu.global.common.entity.enumerated.EnumParser;

public enum PointType {
    EARNED,     // 적립
    USED;       // 사용

    public static PointType from(String value) {
        return EnumParser.fromString(PointType.class, value, "PointType");
    }
}