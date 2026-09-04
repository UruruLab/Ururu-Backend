package com.ururulab.ururu.member.domain.entity.enumerated;

public enum Role {
    MEMBER,
    /** Legacy database value. It is treated as MEMBER and must not be assigned to new accounts. */
    @Deprecated
    NORMAL,
    /** Legacy database value. It is treated as MEMBER and must not be assigned to new accounts. */
    @Deprecated
    STAFF,
    ADMIN
}
