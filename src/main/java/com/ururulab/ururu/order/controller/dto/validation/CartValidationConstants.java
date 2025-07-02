package com.ururulab.ururu.order.controller.dto.validation;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CartValidationConstants {
    public static final int QUANTITY_MIN = 1;
    public static final int QUANTITY_MAX = 999;

    public static final int QUANTITY_CHANGE_MIN = -999;
    public static final int QUANTITY_CHANGE_MAX = 999;
}