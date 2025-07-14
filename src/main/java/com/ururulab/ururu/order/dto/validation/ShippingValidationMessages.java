package com.ururulab.ururu.order.dto.validation;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ShippingValidationMessages {

    // 운송장 번호 등록 관련
    public static final String TRACKING_NUMBER_REQUIRED = "운송장 번호는 필수입니다.";
    public static final String TRACKING_NUMBER_MAX_LENGTH = "운송장 번호는 50자를 초과할 수 없습니다.";
}