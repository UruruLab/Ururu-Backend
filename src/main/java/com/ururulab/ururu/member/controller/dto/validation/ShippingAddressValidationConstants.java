package com.ururulab.ururu.member.controller.dto.validation;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ShippingAddressValidationConstants {
    public static final int LABEL_MIN_LENGTH = 1;
    public static final int LABEL_MAX_LENGTH = 30;
    public static final int PHONE_MIN_LENGTH = 10;
    public static final int PHONE_MAX_LENGTH = 20;
    public static final int ZONECODE_LENGTH = 5;
    public static final int ADDRESS1_MIN_LENGTH = 5;
    public static final int ADDRESS1_MAX_LENGTH = 255;
    public static final int ADDRESS2_MAX_LENGTH = 255;
}
