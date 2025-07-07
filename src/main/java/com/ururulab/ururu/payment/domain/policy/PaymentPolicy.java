package com.ururulab.ururu.payment.domain.policy;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PaymentPolicy {

    // 정책 상수
    public static final int MIN_AMOUNT = 0;
    public static final int MAX_AMOUNT = 100_000_000;
    public static final int PAYMENT_KEY_MAX_LENGTH = 200;

    // 에러 메시지
    public static final String MEMBER_REQUIRED = "회원 정보는 필수입니다.";
    public static final String ORDER_REQUIRED = "주문 정보는 필수입니다.";
    public static final String TOTAL_AMOUNT_REQUIRED = "총 금액은 필수입니다.";
    public static final String TOTAL_AMOUNT_MIN = "총 금액은 0원 이상이어야 합니다.";
    public static final String TOTAL_AMOUNT_MAX = "총 금액은 1억 이하만 가능합니다.";
    public static final String AMOUNT_REQUIRED = "결제 금액은 필수입니다.";
    public static final String AMOUNT_MIN = "결제 금액은 0원 이상이어야 합니다.";
    public static final String AMOUNT_MAX = "결제 금액은 1억 이하만 가능합니다.";
    public static final String POINT_REQUIRED = "포인트는 필수입니다.";
    public static final String POINT_MIN = "포인트는 0 이상이어야 합니다.";
    public static final String POINT_MAX = "포인트는 1억 이하만 가능합니다.";
    public static final String AMOUNT_MISMATCH = "총 금액은 결제금액 + 포인트와 일치해야 합니다.";
    public static final String PAYMENT_KEY_REQUIRED = "결제 키는 필수입니다.";
    public static final String PAY_METHOD_REQUIRED = "결제 수단은 필수입니다.";
    public static final String PAYMENT_AMOUNT_MISMATCH = "결제 금액이 일치하지 않습니다.";
    public static final String ALREADY_PAID = "이미 결제 완료된 상태입니다.";
    public static final String ALREADY_REFUNDED = "환불된 결제는 완료 처리할 수 없습니다.";
    public static final String CANNOT_REFUND_INVALID_STATUS = "결제 완료 또는 부분환불 상태에서만 환불할 수 있습니다.";
    public static final String CANNOT_FAIL_PAID = "결제 완료된 상태에서는 실패 처리할 수 없습니다.";
    public static final String APPROVED_AT_REQUIRED = "결제 승인 시간은 필수입니다.";
    public static final String CANCELLED_AT_REQUIRED = "취소 시간은 필수입니다.";
    public static final String CANNOT_UPDATE_PAID = "이미 결제 완료된 상태에서는 정보를 변경할 수 없습니다.";
    public static final String CANNOT_UPDATE_REFUNDED = "환불된 결제는 정보를 변경할 수 없습니다.";
    public static final String CANNOT_PARTIAL_REFUND_INVALID_STATUS = "결제 완료 또는 부분환불 상태에서만 부분환불 처리할 수 있습니다.";
    public static final String CANNOT_UPDATE_PARTIAL_REFUNDED = "부분환불된 결제는 정보를 변경할 수 있습니다.";
    public static final String ALREADY_PARTIAL_REFUNDED = "이미 부분환불 상태입니다.";
}