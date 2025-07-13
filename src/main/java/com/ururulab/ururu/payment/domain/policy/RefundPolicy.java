package com.ururulab.ururu.payment.domain.policy;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RefundPolicy {

    // 정책 상수
    public static final int ID_LENGTH = 36;
    public static final int MIN_AMOUNT = 0;
    public static final int REASON_MAX_LENGTH = 255;
    public static final int REJECT_REASON_MAX_LENGTH = 255;
    public static final int RETURN_TRACKING_NUMBER_MAX_LENGTH = 50;

    // 에러 메시지
    public static final String PAYMENT_REQUIRED = "결제 정보는 필수입니다.";
    public static final String NOT_REFUNDABLE = "환불 가능한 결제가 아닙니다.";
    public static final String TYPE_REQUIRED = "환불 유형은 필수입니다.";
    public static final String REASON_REQUIRED = "환불 사유는 필수입니다.";
    public static final String AMOUNT_REQUIRED = "환불 금액은 필수입니다.";
    public static final String AMOUNT_MIN = "환불 금액은 0원 이상이어야 합니다.";
    public static final String AMOUNT_EXCEEDS_PAYMENT = "환불 금액이 결제 금액을 초과할 수 없습니다.";
    public static final String REFUNDED_AT_REQUIRED = "환불 완료 시간은 필수입니다.";
    public static final String REJECT_REASON_REQUIRED = "거절 사유는 필수입니다.";
    public static final String INVALID_STATUS_FOR_APPROVAL = "승인 가능한 상태가 아닙니다.";
    public static final String INVALID_STATUS_FOR_REJECTION = "거절 가능한 상태가 아닙니다.";
    public static final String INVALID_STATUS_FOR_COMPLETION = "완료 처리 가능한 상태가 아닙니다.";
    public static final String INVALID_STATUS_FOR_FAILURE = "실패 처리 가능한 상태가 아닙니다.";
    public static final String REFUND_ITEM_REQUIRED = "환불 아이템은 필수입니다.";
    public static final String RETURN_TRACKING_NUMBER_REQUIRED = "물건 반송을 위한 운송장 번호는 필수입니다.";
    public static final String RETURN_TRACKING_NUMBER_TOO_LONG = "운송장 번호는 " + RETURN_TRACKING_NUMBER_MAX_LENGTH + "자를 초과할 수 없습니다.";
}