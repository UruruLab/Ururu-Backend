package com.ururulab.ururu.order.domain.policy;

import lombok.experimental.UtilityClass;

@UtilityClass
public class OrderHistoryPolicy {

    // 정책 상수
    public static final int COMMENT_MAX_LENGTH = 255;

    // 에러 메시지
    public static final String ORDER_REQUIRED = "주문 정보는 필수입니다.";
    public static final String STATUS_REQUIRED = "주문 상태는 필수입니다.";
}