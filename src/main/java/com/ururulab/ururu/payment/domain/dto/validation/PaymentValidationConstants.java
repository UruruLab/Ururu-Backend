package com.ururulab.ururu.payment.domain.dto.validation;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PaymentValidationConstants {
    public static final int USE_POINTS_MIN = 0;
    public static final int USE_POINTS_MAX = 100_000_000;
    public static final int PHONE_MAX_LENGTH = 20;
    public static final int ZONECODE_LENGTH = 5;
    public static final int ADDRESS_MAX_LENGTH = 255;
}