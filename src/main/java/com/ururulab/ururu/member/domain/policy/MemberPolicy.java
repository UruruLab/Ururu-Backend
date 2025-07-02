package com.ururulab.ururu.member.domain.policy;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MemberPolicy {
    public static final int NICKNAME_MAX_LENGTH = 50;
    public static final int EMAIL_MAX_LENGTH = 100;
    public static final int SOCIAL_ID_MAX_LENGTH = 100;
    public static final int PHONE_STRING_MAX_LENGTH = 20;
}
