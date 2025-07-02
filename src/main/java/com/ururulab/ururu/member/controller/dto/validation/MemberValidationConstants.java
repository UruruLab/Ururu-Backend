package com.ururulab.ururu.member.controller.dto.validation;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MemberValidationConstants {
    public static final int NICKNAME_MIN_LENGTH = 2;
    public static final int NICKNAME_MAX_LENGTH = 50;

    public static final int EMAIL_MAX_LENGTH = 100;

    public static final int PHONE_STRING_MAX_LENGTH = 20;

    public static final int SOCIAL_ID_MAX_LENGTH = 100;

    public static final int MIN_AGE = 14;
    public static final int MAX_AGE = 100;
}
