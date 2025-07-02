package com.ururulab.ururu.product.controller.dto.validation;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ProductValidationConstants {
    // Product 필드 길이 제한
    public static final int PRODUCT_NAME_MAX = 100;
    public static final int PRODUCT_DESCRIPTION_MAX = 1000;

    // ProductOption 필드 길이 제한 (필요시 추가)
    public static final int PRODUCT_OPTION_NAME_MAX = 100;
    public static final int FULL_INGREDIENTS_MAX = 1000;

    // ProductNotice 필드 길이 제한
    public static final int CAPACITY_MAX = 50;
    public static final int SPEC_MAX = 100;
    public static final int EXPIRY_MAX = 100;
    public static final int MANUFACTURER_MAX = 100;
    public static final int RESPONSIBLE_SELLER_MAX = 100;
    public static final int COUNTRY_OF_ORIGIN_MAX = 50;
    public static final int CAUTION_MAX = 1000;
    public static final int WARRANTY_MAX = 1000;
    public static final int USAGE_MAX = 1000;
    public static final int CUSTOMER_SERVICE_NUMBER_MAX = 30;
}
