package com.ururulab.ururu.seller.domain.dto.validation;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SellerValidationPatterns {
    public static final String BUSINESS_NUMBER_PATTERN = "^[0-9]{10}$";
    public static final String PASSWORD_PATTERN = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$";
    public static final String PHONE_PATTERN = "^[0-9]{10,11}$";
}