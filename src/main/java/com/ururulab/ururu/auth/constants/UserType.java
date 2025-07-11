package com.ururulab.ururu.auth.constants;

/**
 * 사용자 타입 enum.
 * 타입 안전성을 보장하고 허용된 값만 사용할 수 있도록 합니다.
 */
public enum UserType {
    MEMBER("MEMBER"),
    SELLER("SELLER");

    private final String value;

    UserType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static UserType fromString(String value) {
        for (UserType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown user type: " + value);
    }
} 