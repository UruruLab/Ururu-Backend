package com.ururulab.ururu.seller.domain.constant;

import lombok.experimental.UtilityClass;

/**
 * 판매자 도메인 관련 상수 정의.
 * 도메인 계층에서 사용하는 상수들을 중앙화하여 관리합니다.
 */
@UtilityClass
public class SellerConstants {
    
    // 검증 관련 상수
    public static final int NAME_MAX_LENGTH = 50;
    public static final int BUSINESS_NAME_MAX_LENGTH = 100;
    public static final int OWNER_NAME_MAX_LENGTH = 50;
    public static final int BUSINESS_NUMBER_LENGTH = 10;
    public static final int EMAIL_MAX_LENGTH = 100;
    public static final int PASSWORD_MAX_LENGTH = 50;
    public static final int PHONE_MAX_LENGTH = 20;
    public static final int ZONECODE_LENGTH = 5;
    public static final int ADDRESS_MAX_LENGTH = 100;
    public static final int MAIL_ORDER_NUMBER_MAX_LENGTH = 50;
    
    // DB 컬럼 길이
    public static final int NAME_COLUMN_LENGTH = 50;
    public static final int BUSINESS_NAME_COLUMN_LENGTH = 100;
    public static final int OWNER_NAME_COLUMN_LENGTH = 50;
    public static final int BUSINESS_NUMBER_COLUMN_LENGTH = 10;
    public static final int EMAIL_COLUMN_LENGTH = 100;
    public static final int PASSWORD_COLUMN_LENGTH = 255;
    public static final int PHONE_COLUMN_LENGTH = 20;
    public static final int ZONECODE_COLUMN_LENGTH = 5;
    public static final int IMAGE_COLUMN_LENGTH = 255;
    public static final int ADDRESS_COLUMN_LENGTH = 255;
    public static final int MAIL_ORDER_NUMBER_COLUMN_LENGTH = 50;
    
    // 정규식 패턴
    public static final String BUSINESS_NUMBER_PATTERN = "^[0-9]{10}$";
    public static final String PASSWORD_PATTERN = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$";
    public static final String PHONE_PATTERN = "^[0-9]{10,11}$";
    public static final String ZONECODE_PATTERN = "^[0-9]{5}$";
    public static final String EMAIL_PATTERN = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
    
    // 에러 메시지
    public static final String NAME_REQUIRED = "브랜드명은 필수입니다.";
    public static final String NAME_SIZE = "브랜드명은 " + NAME_MAX_LENGTH + "자 이하여야 합니다.";
    public static final String BUSINESS_NAME_REQUIRED = "사업자명은 필수입니다.";
    public static final String BUSINESS_NAME_SIZE = "사업자명은 " + BUSINESS_NAME_MAX_LENGTH + "자 이하여야 합니다.";
    public static final String OWNER_NAME_REQUIRED = "대표 CEO명은 필수입니다.";
    public static final String OWNER_NAME_SIZE = "대표 CEO명은 " + OWNER_NAME_MAX_LENGTH + "자 이하여야 합니다.";
    public static final String BUSINESS_NUMBER_REQUIRED = "사업자등록번호는 필수입니다.";
    public static final String BUSINESS_NUMBER_SIZE = "사업자등록번호는 " + BUSINESS_NUMBER_LENGTH + "자여야 합니다.";
    public static final String BUSINESS_NUMBER_PATTERN_ERROR = "사업자등록번호는 숫자만 입력 가능합니다.";
    public static final String EMAIL_REQUIRED = "이메일은 필수입니다.";
    public static final String EMAIL_FORMAT = "올바른 이메일 형식이 아닙니다.";
    public static final String EMAIL_SIZE = "이메일은 " + EMAIL_MAX_LENGTH + "자 이하여야 합니다.";
    public static final String PASSWORD_REQUIRED = "비밀번호는 필수입니다.";
    public static final String PASSWORD_SIZE = "비밀번호는 " + PASSWORD_MAX_LENGTH + "자 이하여야 합니다.";
    public static final String PASSWORD_PATTERN_ERROR = "비밀번호는 영문, 숫자, 특수문자를 포함하여 8자 이상이어야 합니다.";
    public static final String PHONE_REQUIRED = "전화번호는 필수입니다.";
    public static final String PHONE_FORMAT = "올바른 전화번호 형식이 아닙니다. (10-11자리 숫자)";
    public static final String PHONE_SIZE = "전화번호는 " + PHONE_MAX_LENGTH + "자 이하여야 합니다.";
    public static final String ZONECODE_REQUIRED = "우편번호는 필수입니다.";
    public static final String ZONECODE_SIZE = "우편번호는 " + ZONECODE_LENGTH + "자리여야 합니다.";
    public static final String ZONECODE_PATTERN_ERROR = "우편번호는 " + ZONECODE_LENGTH + "자리 숫자만 입력 가능합니다.";
    public static final String ADDRESS1_REQUIRED = "주소는 필수입니다.";
    public static final String ADDRESS1_SIZE = "주소는 " + ADDRESS_MAX_LENGTH + "자 이하여야 합니다.";
    public static final String ADDRESS2_REQUIRED = "상세 주소는 필수입니다.";
    public static final String ADDRESS2_SIZE = "상세 주소는 " + ADDRESS_MAX_LENGTH + "자 이하여야 합니다.";
    public static final String MAIL_ORDER_NUMBER_REQUIRED = "통신판매업 신고번호는 필수입니다.";
    public static final String MAIL_ORDER_NUMBER_SIZE = "통신판매업 신고번호는 " + MAIL_ORDER_NUMBER_MAX_LENGTH + "자 이하여야 합니다.";
} 