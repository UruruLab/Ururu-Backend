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

    // 시스템 메시지
    public static final String ORDER_CREATION_MESSAGE = "주문이 생성되었습니다.";
}
