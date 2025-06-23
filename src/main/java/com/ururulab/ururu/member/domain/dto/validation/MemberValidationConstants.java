package com.ururulab.ururu.member.domain.dto.validation;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MemberValidationConstants {
    public static final int NICKNAME_MIN_LENGTH = 2;
    public static final int NICKNAME_MAX_LENGTH = 50;
    public static final int PHONE_STRING_MAX_LENGTH = 20;
    public static final int MIN_AGE = 14;
    public static final int MAX_AGE = 100;
}
