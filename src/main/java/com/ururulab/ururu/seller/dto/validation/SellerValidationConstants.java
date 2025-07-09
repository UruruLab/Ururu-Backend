package com.ururulab.ururu.seller.dto.validation;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SellerValidationConstants {
    // 사용자 입력 검증용
    public static final int NAME_MAX_LENGTH = 50;
    public static final int BUSINESS_NAME_MAX_LENGTH = 100;
    public static final int OWNER_NAME_MAX_LENGTH = 50;
    public static final int BUSINESS_NUMBER_LENGTH = 10;
    public static final int EMAIL_MAX_LENGTH = 100;
    public static final int PASSWORD_MAX_LENGTH = 50;
    public static final int PHONE_MAX_LENGTH = 20;
    public static final int IMAGE_MAX_LENGTH = 50;
    public static final int ADDRESS_MAX_LENGTH = 100;
    public static final int MAIL_ORDER_NUMBER_MAX_LENGTH = 50;

    // DB 컬럼 길이용
    public static final int NAME_COLUMN_LENGTH = 50;
    public static final int BUSINESS_NAME_COLUMN_LENGTH = 100;
    public static final int OWNER_NAME_COLUMN_LENGTH = 50;
    public static final int BUSINESS_NUMBER_COLUMN_LENGTH = 10;
    public static final int EMAIL_COLUMN_LENGTH = 100;
    public static final int PASSWORD_COLUMN_LENGTH = 255;
    public static final int PHONE_COLUMN_LENGTH = 20;
    public static final int IMAGE_COLUMN_LENGTH = 255;
    public static final int ADDRESS_COLUMN_LENGTH = 255;
    public static final int MAIL_ORDER_NUMBER_COLUMN_LENGTH = 50;
}