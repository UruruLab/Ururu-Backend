package com.ururulab.ururu.auth.service;

import com.ururulab.ururu.auth.jwt.JwtTokenProvider;
import com.ururulab.ururu.auth.storage.TokenBlacklistStorage;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 토큰 검증 서비스.
 * JWT 토큰의 유효성 검사, 사용자 정보 추출, 블랙리스트 확인 등을 통합 관리합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public final class TokenValidator {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistStorage tokenBlacklistStorage;

    /**
     * 액세스 토큰을 검증하고 사용자 정보를 추출합니다.
     *
     * @param accessToken 검증할 액세스 토큰
     * @return 토큰 검증 결과 (성공 시 사용자 정보 포함)
     * @throws BusinessException 토큰이 유효하지 않은 경우
     */
    public TokenValidationResult validateAccessToken(final String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new BusinessException(ErrorCode.MISSING_AUTHORIZATION_HEADER);
        }

        // 1. JWT 토큰 구조 및 서명 검증
        if (!jwtTokenProvider.validateToken(accessToken)) {
            throw new BusinessException(ErrorCode.INVALID_JWT_TOKEN);
        }

        // 2. 토큰 타입 확인 (액세스 토큰인지)
        if (!jwtTokenProvider.isAccessToken(accessToken)) {
            throw new BusinessException(ErrorCode.INVALID_JWT_TOKEN);
        }

        // 3. 토큰 만료 확인
        if (jwtTokenProvider.isTokenExpired(accessToken)) {
            throw new BusinessException(ErrorCode.EXPIRED_JWT_TOKEN);
        }

        // 4. 블랙리스트 확인
        final String tokenId = jwtTokenProvider.getTokenId(accessToken);
        if (tokenBlacklistStorage.isTokenBlacklisted(tokenId)) {
            throw new BusinessException(ErrorCode.INVALID_JWT_TOKEN);
        }

        // 5. 사용자 정보 추출
        final Long userId = jwtTokenProvider.getMemberId(accessToken);
        final String userType = jwtTokenProvider.getUserType(accessToken);

        log.debug("Access token validation successful for user: {} (type: {})", userId, userType);

        return TokenValidationResult.of(userId, userType, tokenId);
    }

    /**
     * 리프레시 토큰을 검증하고 사용자 정보를 추출합니다.
     *
     * @param refreshToken 검증할 리프레시 토큰
     * @return 토큰 검증 결과 (성공 시 사용자 정보 포함)
     * @throws BusinessException 토큰이 유효하지 않은 경우
     */
    public TokenValidationResult validateRefreshToken(final String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.MISSING_REFRESH_TOKEN);
        }

        // 1. JWT 토큰 구조 및 서명 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_JWT_TOKEN);
        }

        // 2. 토큰 타입 확인 (리프레시 토큰인지)
        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 3. 토큰 만료 확인
        if (jwtTokenProvider.isTokenExpired(refreshToken)) {
            throw new BusinessException(ErrorCode.EXPIRED_JWT_TOKEN);
        }

        // 4. 블랙리스트 확인
        final String tokenId = jwtTokenProvider.getTokenId(refreshToken);
        if (tokenBlacklistStorage.isTokenBlacklisted(tokenId)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 5. 사용자 정보 추출
        final Long userId = jwtTokenProvider.getMemberId(refreshToken);
        final String userType = jwtTokenProvider.getUserType(refreshToken);

        log.debug("Refresh token validation successful for user: {} (type: {})", userId, userType);

        return TokenValidationResult.of(userId, userType, tokenId);
    }

    /**
     * 토큰 검증 결과를 담는 불변 객체.
     */
    public record TokenValidationResult(Long userId, String userType, String tokenId) {
        public static TokenValidationResult of(final Long userId, final String userType, final String tokenId) {
            return new TokenValidationResult(userId, userType, tokenId);
        }
    }
} 