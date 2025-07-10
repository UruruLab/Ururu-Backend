package com.ururulab.ururu.auth.constants;

/**
 * 사용자 역할 enum.
 * 타입 안전성을 보장하고 허용된 값만 사용할 수 있도록 합니다.
 * Member 엔티티의 Role enum과 호환되도록 설계되었습니다.
 */
public enum UserRole {
    NORMAL("NORMAL"),
    STAFF("STAFF"),
    ADMIN("ADMIN"),
    SELLER("SELLER");

    private final String value;

    UserRole(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static UserRole fromString(String value) {
        for (UserRole role : values()) {
            if (role.value.equals(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown user role: " + value);
    }
} 