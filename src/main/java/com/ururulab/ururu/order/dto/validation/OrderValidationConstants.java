package com.ururulab.ururu.order.dto.validation;

import lombok.experimental.UtilityClass;

@UtilityClass
public class OrderValidationConstants {
    public static final int QUANTITY_MIN = 1;
    public static final int QUANTITY_MAX = 999;
    public static final int ORDER_ITEMS_MAX = 50;
    public static final int CART_ITEMS_MAX = 50;
}