package com.ururulab.ururu.payment.domain.policy;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PointTransactionPolicy {

    // 정책 상수
    public static final Integer MIN_EARNED_AMOUNT = 1;
    public static final Integer MIN_USED_AMOUNT = 1;
    public static final Integer REASON_MAX_LENGTH = 100;

    // 에러 메시지
    public static final String MEMBER_REQUIRED = "회원 정보는 필수입니다.";
    public static final String TYPE_REQUIRED = "포인트 타입은 필수입니다.";
    public static final String SOURCE_REQUIRED = "포인트 발생 경로는 필수입니다.";
    public static final String AMOUNT_REQUIRED = "포인트 금액은 필수입니다.";
    public static final String EARNED_AMOUNT_MIN = "적립 포인트는 0보다 커야 합니다.";
    public static final String USED_AMOUNT_MIN = "사용 포인트는 0보다 커야 합니다.";
}