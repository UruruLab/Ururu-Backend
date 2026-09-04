package com.ururulab.ururu.auth.constants;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;

/** Application permissions. ADMIN is the platform operator, not a seller administrator. */
public enum UserRole {
    MEMBER,
    SELLER,
    ADMIN;

    public String getValue() {
        return name();
    }

    public static UserRole fromString(final String value) {
        try {
            return valueOf(value);
        } catch (final RuntimeException e) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "Unknown user role: " + value);
        }
    }
}
