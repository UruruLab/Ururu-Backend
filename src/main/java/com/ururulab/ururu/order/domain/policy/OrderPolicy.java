package com.ururulab.ururu.order.domain.policy;

import lombok.experimental.UtilityClass;

@UtilityClass
public class OrderPolicy {

    // 정책 상수
    public static final int ID_LENGTH = 36;
    public static final int PHONE_MAX_LENGTH = 20;
    public static final int ZONECODE_MAX_LENGTH = 5;
    public static final int ADDRESS_MAX_LENGTH = 255;
    public static final int TRACKING_NUMBER_MAX_LENGTH = 50;

    // 에러 메시지
    public static final String GROUPBUY_REQUIRED = "공동구매 정보는 필수입니다.";
    public static final String MEMBER_REQUIRED = "회원 정보는 필수입니다.";
    public static final String ORDER_ITEM_REQUIRED = "주문 아이템은 필수입니다.";
    public static final String STATUS_REQUIRED = "주문 상태는 필수입니다.";
    public static final String PHONE_REQUIRED = "전화번호는 필수입니다.";
    public static final String PHONE_TOO_LONG = "전화번호는 " + PHONE_MAX_LENGTH + "자를 초과할 수 없습니다.";
    public static final String ZONECODE_REQUIRED = "우편번호는 필수입니다.";
    public static final String ZONECODE_TOO_LONG = "우편번호는 " + ZONECODE_MAX_LENGTH + "자를 초과할 수 없습니다.";
    public static final String ADDRESS1_REQUIRED = "주소는 필수입니다.";
    public static final String ADDRESS1_TOO_LONG = "주소는 " + ADDRESS_MAX_LENGTH + "자를 초과할 수 없습니다.";
    public static final String ADDRESS2_TOO_LONG = "상세주소는 " + ADDRESS_MAX_LENGTH + "자를 초과할 수 없습니다.";
    public static final String TRACKING_NUMBER_TOO_LONG = "운송장번호는 " + TRACKING_NUMBER_MAX_LENGTH + "자를 초과할 수 없습니다.";

    // 시스템 메시지
    public static final String ORDER_CREATION_MESSAGE = "주문이 생성되었습니다.";
    public static final String PAYMENT_INFO_ONLY_PENDING = "PENDING 상태에서만 결제 정보를 완성할 수 있습니다.";
    public static final String PAYMENT_INFO_COMPLETED = "결제 정보가 완성되었습니다.";

}
