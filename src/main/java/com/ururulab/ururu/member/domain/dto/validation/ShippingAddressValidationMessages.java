package com.ururulab.ururu.member.domain.dto.validation;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ShippingAddressValidationMessages {
    public static final String LABEL_REQUIRED = "배송지명은 필수입니다.";
    public static final String LABEL_SIZE = "배송지명은 1-30자 사이여야 합니다.";
    public static final String LABEL_PATTERN_INVALID = "배송지명은 한글, 영문, 숫자, 공백만 사용 가능합니다.";

    public static final String PHONE_REQUIRED = "연락처는 필수입니다.";
    public static final String PHONE_SIZE = "연락처는 10-20자 사이여야 합니다.";
    public static final String PHONE_PATTERN_INVALID = "올바른 연락처 형식이 아닙니다. (숫자, 하이픈만 허용)";

    public static final String ZONECODE_REQUIRED = "우편번호는 필수입니다.";
    public static final String ZONECODE_SIZE = "우편번호는 5자리여야 합니다.";
    public static final String ZONECODE_PATTERN_INVALID = "우편번호는 5자리 숫자만 입력 가능합니다.";

    public static final String ADDRESS1_REQUIRED = "기본주소는 필수입니다.";
    public static final String ADDRESS1_SIZE = "기본주소는 5-255자 사이여야 합니다.";

    public static final String ADDRESS2_SIZE = "상세주소는 255자 이하여야 합니다.";
}
