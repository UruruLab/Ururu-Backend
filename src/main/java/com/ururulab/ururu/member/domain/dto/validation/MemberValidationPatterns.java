package com.ururulab.ururu.member.domain.dto.validation;

public class MemberValidationPatterns {
    public static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    public static final String PHONE_PATTERN = "^[0-9]{10,11}$";
    public static final String BIRTH_PATTERN = "^\\d{4}-\\d{2}-\\d{2}$";
    public static final String NICKNAME_PATTERN = "^[가-힣a-zA-Z0-9._-]+$";
    public static final String VERIFICATION_CODE_PATTERN = "^[0-9]{6}$";
    public static final String GENDER_PATTERN = "MALE|FEMALE|NONE";
}
