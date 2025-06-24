package com.ururulab.ururu.member.domain.dto.validation;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ShippingAddressValidationPatterns {
    public static final String LABEL_PATTERN = "^[가-힣a-zA-Z0-9\\s]+$";
    public static final String PHONE_PATTERN = "^[0-9-]+$";
    public static final String ZONECODE_PATTERN = "^[0-9]{5}$";
}
