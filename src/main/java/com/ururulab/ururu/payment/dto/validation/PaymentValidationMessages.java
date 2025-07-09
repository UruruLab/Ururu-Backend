package com.ururulab.ururu.payment.dto.validation;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PaymentValidationMessages {

    // 결제 요청 관련
    public static final String ORDER_ID_REQUIRED = "주문 ID는 필수입니다.";
    public static final String USE_POINTS_REQUIRED = "사용 포인트는 필수입니다.";
    public static final String USE_POINTS_MIN = "사용 포인트는 0 이상이어야 합니다.";
    public static final String USE_POINTS_MAX = "사용 포인트는 1억 포인트를 초과할 수 없습니다.";

    // 배송지 정보 관련
    public static final String PHONE_REQUIRED = "연락처는 필수입니다.";
    public static final String PHONE_MAX_LENGTH = "연락처는 20자를 초과할 수 없습니다.";
    public static final String ZONECODE_REQUIRED = "우편번호는 필수입니다.";
    public static final String ZONECODE_LENGTH = "우편번호는 5자리여야 합니다.";
    public static final String ADDRESS1_REQUIRED = "주소는 필수입니다.";
    public static final String ADDRESS1_MAX_LENGTH = "주소는 255자를 초과할 수 없습니다.";
    public static final String ADDRESS2_MAX_LENGTH = "상세주소는 255자를 초과할 수 없습니다.";

    // 결제 승인 관련
    public static final String AMOUNT_REQUIRED = "결제 금액은 필수입니다.";
    public static final String PAYMENT_KEY_REQUIRED = "결제 키는 필수입니다.";
}
