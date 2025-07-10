package com.ururulab.ururu.auth.jwt.token;

import com.ururulab.ururu.auth.jwt.JwtProperties;
import com.ururulab.ururu.auth.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Access Token 생성을 전담하는 클래스.
 * 단일책임원칙에 따라 Access Token 생성만 담당합니다.
 */
@Component
@RequiredArgsConstructor
public final class AccessTokenGenerator {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    /**
     * Access Token을 생성합니다.
     *
     * @param userId 사용자 ID
     * @param email 사용자 이메일
     * @param role 사용자 역할
     * @param userType 사용자 타입 (MEMBER/SELLER)
     * @return 생성된 Access Token
     */
    public String generateAccessToken(final Long userId, final String email, final String role, final String userType) {
        validateParameters(userId, email, role, userType);
        return jwtTokenProvider.generateAccessToken(userId, email, role, userType);
    }

    /**
     * Access Token의 만료 시간을 반환합니다.
     *
     * @return Access Token 만료 시간 (초)
     */
    public Long getExpirySeconds() {
        return jwtTokenProvider.getAccessTokenExpiry();
    }

    private void validateParameters(final Long userId, final String email, final String role, final String userType) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or blank");
        }
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Role cannot be null or blank");
        }
        if (userType == null || userType.isBlank()) {
            throw new IllegalArgumentException("User type cannot be null or blank");
        }
    }
} 