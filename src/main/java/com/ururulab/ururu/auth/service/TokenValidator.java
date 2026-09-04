package com.ururulab.ururu.auth.service;

import com.ururulab.ururu.auth.constants.UserRole;
import com.ururulab.ururu.auth.constants.UserType;
import com.ururulab.ururu.auth.jwt.JwtTokenProvider;
import com.ururulab.ururu.auth.storage.TokenBlacklistStorage;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public final class TokenValidator {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistStorage tokenBlacklistStorage;

    public TokenValidationResult validateAccessToken(final String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new BusinessException(ErrorCode.MISSING_AUTHORIZATION_HEADER);
        }
        if (jwtTokenProvider.isTokenExpired(accessToken)) {
            throw new BusinessException(ErrorCode.EXPIRED_JWT_TOKEN);
        }
        if (!jwtTokenProvider.isAccessToken(accessToken)) {
            throw new BusinessException(ErrorCode.INVALID_JWT_TOKEN);
        }

        final String tokenId = jwtTokenProvider.getTokenId(accessToken);
        final Long userId = jwtTokenProvider.getMemberId(accessToken);
        final String userType = jwtTokenProvider.getUserType(accessToken);
        final String role = normalizeRole(jwtTokenProvider.getRole(accessToken));
        validateTokenId(tokenId, ErrorCode.INVALID_JWT_TOKEN);
        validateIdentity(userType, role);

        if (tokenBlacklistStorage.isTokenBlacklisted(tokenId)
                || tokenBlacklistStorage.isUserTokenRevoked(
                        userId,
                        userType,
                        jwtTokenProvider.getIssuedAtMillis(accessToken)
                )) {
            throw new BusinessException(ErrorCode.INVALID_JWT_TOKEN);
        }

        log.debug("Access token validation successful for user: {} (type: {})", userId, userType);
        return TokenValidationResult.of(userId, userType, tokenId, role);
    }

    public TokenValidationResult validateRefreshToken(final String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.MISSING_REFRESH_TOKEN);
        }
        if (jwtTokenProvider.isTokenExpired(refreshToken)) {
            throw new BusinessException(ErrorCode.EXPIRED_JWT_TOKEN);
        }
        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        final String tokenId = jwtTokenProvider.getTokenId(refreshToken);
        validateTokenId(tokenId, ErrorCode.INVALID_REFRESH_TOKEN);
        if (tokenBlacklistStorage.isTokenBlacklisted(tokenId)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        final Long userId = jwtTokenProvider.getMemberId(refreshToken);
        final String userType = jwtTokenProvider.getUserType(refreshToken);
        validateUserType(userType);
        if (tokenBlacklistStorage.isUserTokenRevoked(
                userId, userType, jwtTokenProvider.getIssuedAtMillis(refreshToken))) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        log.debug("Refresh token validation successful for user: {} (type: {})", userId, userType);
        return TokenValidationResult.of(userId, userType, tokenId, null);
    }

    private void validateIdentity(final String userType, final String role) {
        final UserType parsedUserType = validateUserType(userType);
        final UserRole parsedRole;
        try {
            parsedRole = UserRole.valueOf(role);
        } catch (final RuntimeException e) {
            throw new BusinessException(ErrorCode.INVALID_JWT_TOKEN);
        }
        final boolean validSeller = parsedUserType == UserType.SELLER && parsedRole == UserRole.SELLER;
        final boolean validMember = parsedUserType == UserType.MEMBER
                && (parsedRole == UserRole.MEMBER || parsedRole == UserRole.ADMIN);
        if (!validSeller && !validMember) {
            throw new BusinessException(ErrorCode.INVALID_JWT_TOKEN);
        }
    }

    private String normalizeRole(final String role) {
        return "NORMAL".equals(role) || "STAFF".equals(role) ? UserRole.MEMBER.getValue() : role;
    }

    private UserType validateUserType(final String userType) {
        try {
            return UserType.fromString(userType);
        } catch (final RuntimeException e) {
            throw new BusinessException(ErrorCode.INVALID_JWT_TOKEN);
        }
    }

    private void validateTokenId(final String tokenId, final ErrorCode errorCode) {
        if (tokenId == null || tokenId.isBlank()) {
            throw new BusinessException(errorCode);
        }
    }

    public record TokenValidationResult(Long userId, String userType, String tokenId, String role) {
        public static TokenValidationResult of(final Long userId, final String userType, final String tokenId) {
            return new TokenValidationResult(userId, userType, tokenId, null);
        }

        public static TokenValidationResult of(
                final Long userId,
                final String userType,
                final String tokenId,
                final String role
        ) {
            return new TokenValidationResult(userId, userType, tokenId, role);
        }
    }
}
