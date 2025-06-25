package com.ururulab.ururu.payment.domain.policy;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RefundPolicy {

    // 정책 상수
    public static final Integer MIN_AMOUNT = 0;
    public static final Integer REASON_MAX_LENGTH = 255;

    // 에러 메시지
    public static final String PAYMENT_REQUIRED = "결제 정보는 필수입니다.";
    public static final String NOT_REFUNDABLE = "환불 가능한 결제가 아닙니다.";
    public static final String REASON_REQUIRED = "환불 사유는 필수입니다.";
    public static final String AMOUNT_REQUIRED = "환불 금액은 필수입니다.";
    public static final String AMOUNT_MIN = "환불 금액은 0원 이상이어야 합니다.";
    public static final String AMOUNT_EXCEEDS_PAYMENT = "환불 금액이 결제 금액을 초과할 수 없습니다.";
    public static final String REFUNDED_AT_REQUIRED = "환불 완료 시간은 필수입니다.";
    public static final String ALREADY_COMPLETED = "이미 환불 완료된 상태입니다.";
    public static final String CANNOT_FAIL_COMPLETED = "완료된 환불은 실패 처리할 수 없습니다.";
}