package com.ururulab.ururu.payment.dto.validation;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RefundValidationConstants {
    public static final int REASON_MAX_LENGTH = 255;
    public static final int REJECT_REASON_MAX_LENGTH = 255;

    public static final String REFUND_ACTION_PATTERN = "^(APPROVE|REJECT)$";
}