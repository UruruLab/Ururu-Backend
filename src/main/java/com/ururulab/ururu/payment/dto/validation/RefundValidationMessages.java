package com.ururulab.ururu.payment.dto.validation;

import lombok.experimental.UtilityClass;

import static com.ururulab.ururu.payment.dto.validation.RefundValidationConstants.RETURN_TRACKING_NUMBER_MAX_LENGTH;

@UtilityClass
public class RefundValidationMessages {

    // 환불 요청 관련
    public static final String TYPE_REQUIRED = "환불 타입은 필수입니다.";
    public static final String TYPE_INVALID = "유효하지 않은 환불 타입입니다.";
    public static final String REASON_REQUIRED = "환불 사유는 필수입니다.";
    public static final String REASON_MAX_LENGTH = "환불 사유는 255자를 초과할 수 없습니다.";

    // 환불 처리 관련
    public static final String ACTION_REQUIRED = "처리 액션은 필수입니다.";
    public static final String ACTION_INVALID = "유효하지 않은 액션입니다.";
    public static final String REJECT_REASON_MAX_LENGTH = "거절 사유는 255자를 초과할 수 없습니다.";
    public static final String RETURN_TRACKING_NUMBER_TOO_LONG = "운송장 번호는 " + RETURN_TRACKING_NUMBER_MAX_LENGTH + "자를 초과할 수 없습니다.";
}
