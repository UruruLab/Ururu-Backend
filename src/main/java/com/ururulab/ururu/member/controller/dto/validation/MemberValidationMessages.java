package com.ururulab.ururu.member.controller.dto.validation;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MemberValidationMessages {
    public static final String NICKNAME_REQUIRED = "닉네임은 필수입니다.";
    public static final String NICKNAME_SIZE = "닉네임은 " +
            MemberValidationConstants.NICKNAME_MIN_LENGTH + "-" +
            MemberValidationConstants.NICKNAME_MAX_LENGTH + "자 사이여야 합니다.";
    public static final String NICKNAME_PATTERN_INVALID = "닉네임은 한글, 영문, 숫자, ., _, - 만 사용 가능합니다.";

    public static final String EMAIL_REQUIRED = "이메일은 필수입니다.";
    public static final String EMAIL_FORMAT = "올바른 이메일 형식이 아닙니다.";
    public static final String EMAIL_SIZE = "이메일은 " +
            MemberValidationConstants.EMAIL_MAX_LENGTH + "자 이하여야 합니다.";

    public static final String PHONE_REQUIRED = "전화번호는 필수입니다.";
    public static final String PHONE_FORMAT = "올바른 전화번호 형식이 아닙니다. (10-11자리 숫자)";
    public static final String PHONE_SIZE = "전화번호는 " +
            MemberValidationConstants.PHONE_STRING_MAX_LENGTH + "자 이하여야 합니다.";

    public static final String BIRTH_REQUIRED = "생년월일은 필수입니다.";
    public static final String BIRTH_INVALID = "생년월일은 현재 날짜보다 이전이어야 합니다.";

    public static final String GENDER_INVALID = "성별이 유효하지 않습니다.";

    public static final String SOCIAL_PROVIDER_REQUIRED = "소셜 제공자는 필수입니다.";
    public static final String SOCIAL_ID_REQUIRED = "소셜 ID는 필수입니다.";
    public static final String SOCIAL_ID_SIZE = "소셜 ID는 " +
            MemberValidationConstants.SOCIAL_ID_MAX_LENGTH + "자 이하여야 합니다.";
}
