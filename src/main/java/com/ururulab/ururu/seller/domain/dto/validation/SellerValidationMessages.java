package com.ururulab.ururu.seller.domain.dto.validation;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SellerValidationMessages {
    public static final String NAME_REQUIRED = "브랜드명은 필수입니다.";
    public static final String NAME_SIZE = "브랜드명은 " +
            SellerValidationConstants.NAME_MAX_LENGTH + "자 이하여야 합니다.";

    public static final String BUSINESS_NAME_REQUIRED = "사업자명은 필수입니다.";
    public static final String BUSINESS_NAME_SIZE = "사업자명은 " +
            SellerValidationConstants.BUSINESS_NAME_MAX_LENGTH + "자 이하여야 합니다.";

    public static final String OWNER_NAME_REQUIRED = "대표 CEO명은 필수입니다.";
    public static final String OWNER_NAME_SIZE = "대표 CEO명은 " +
            SellerValidationConstants.OWNER_NAME_MAX_LENGTH + "자 이하여야 합니다.";

    public static final String BUSINESS_NUMBER_REQUIRED = "사업자등록번호는 필수입니다.";
    public static final String BUSINESS_NUMBER_SIZE = "사업자등록번호는 " +
            SellerValidationConstants.BUSINESS_NUMBER_LENGTH + "자여야 합니다.";
    public static final String BUSINESS_NUMBER_PATTERN = "사업자등록번호는 숫자만 입력 가능합니다.";

    public static final String EMAIL_REQUIRED = "이메일은 필수입니다.";
    public static final String EMAIL_FORMAT = "올바른 이메일 형식이 아닙니다.";
    public static final String EMAIL_SIZE = "이메일은 " +
            SellerValidationConstants.EMAIL_MAX_LENGTH + "자 이하여야 합니다.";

    public static final String PASSWORD_REQUIRED = "비밀번호는 필수입니다.";
    public static final String PASSWORD_PATTERN = "비밀번호는 영문, 숫자, 특수문자를 포함하여 8자 이상이어야 합니다.";

    public static final String PHONE_REQUIRED = "전화번호는 필수입니다.";
    public static final String PHONE_FORMAT = "올바른 전화번호 형식이 아닙니다. (10-11자리 숫자)";
    public static final String PHONE_SIZE = "전화번호는 " +
            SellerValidationConstants.PHONE_MAX_LENGTH + "자 이하여야 합니다.";

    public static final String ADDRESS1_REQUIRED = "주소는 필수입니다.";
    public static final String ADDRESS1_SIZE = "주소는 " +
            SellerValidationConstants.ADDRESS_MAX_LENGTH + "자 이하여야 합니다.";

    public static final String ADDRESS2_SIZE = "상세 주소는 " +
            SellerValidationConstants.ADDRESS_MAX_LENGTH + "자 이하여야 합니다.";

    public static final String MAIL_ORDER_NUMBER_REQUIRED = "통신판매업 신고번호는 필수입니다.";
    public static final String MAIL_ORDER_NUMBER_SIZE = "통신판매업 신고번호는 " +
            SellerValidationConstants.MAIL_ORDER_NUMBER_MAX_LENGTH + "자 이하여야 합니다.";
}