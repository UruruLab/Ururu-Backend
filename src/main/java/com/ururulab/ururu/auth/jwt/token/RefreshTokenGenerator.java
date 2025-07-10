package com.ururulab.ururu.auth.jwt.token;

import com.ururulab.ururu.auth.constants.UserType;
import com.ururulab.ururu.auth.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Refresh Token 생성을 전담하는 클래스.
 * 단일책임원칙에 따라 Refresh Token 생성만 담당합니다.
 */
@Component
@RequiredArgsConstructor
public final class RefreshTokenGenerator {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Refresh Token을 생성합니다.
     *
     * @param userId 사용자 ID
     * @param userType 사용자 타입 (MEMBER/SELLER)
     * @return 생성된 Refresh Token
     */
    public String generateRefreshToken(final Long userId, final UserType userType) {
        validateParameters(userId, userType);
        return jwtTokenProvider.generateRefreshToken(userId, userType);
    }

    /**
     * Refresh Token의 만료 시간을 반환합니다.
     *
     * @return Refresh Token 만료 시간 (초)
     */
    public long getExpirySeconds() {
        return jwtTokenProvider.getRefreshTokenExpirySeconds();
    }

    private void validateParameters(final Long userId, final UserType userType) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (userType == null) {
            throw new IllegalArgumentException("User type cannot be null");
        }
    }
} 