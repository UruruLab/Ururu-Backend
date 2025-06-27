package com.ururulab.ururu.order.domain.dto.validation;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ValidationConstants {
    public static final int QUANTITY_MIN = 1;
    public static final int QUANTITY_MAX = 999;

    public static final int QUANTITY_CHANGE_MIN = -999;
    public static final int QUANTITY_CHANGE_MAX = 999;
}