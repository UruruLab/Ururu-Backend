package com.ururulab.ururu.auth.service;

import com.ururulab.ururu.auth.constants.AuthConstants;
import com.ururulab.ururu.auth.constants.UserRole;
import com.ururulab.ururu.auth.constants.UserType;
import com.ururulab.ururu.auth.dto.response.SocialLoginResponse;
import com.ururulab.ururu.auth.jwt.JwtProperties;
import com.ururulab.ururu.auth.jwt.JwtTokenProvider;
import com.ururulab.ururu.auth.jwt.token.AccessTokenGenerator;
import com.ururulab.ururu.auth.jwt.token.RefreshTokenGenerator;
import com.ururulab.ururu.auth.storage.RefreshTokenStorage;
import com.ururulab.ururu.auth.util.TokenExtractor;
import com.ururulab.ururu.auth.service.TokenValidator;
import com.ururulab.ururu.auth.storage.TokenBlacklistStorage;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * JWT 토큰 갱신 서비스.
 * Refresh Token을 사용하여 새로운 Access Token과 Refresh Token을 발급하는 역할을 담당합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public final class JwtRefreshService {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final AccessTokenGenerator accessTokenGenerator;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final RefreshTokenStorage refreshTokenStorage;
    private final TokenBlacklistStorage tokenBlacklistStorage;
    private final UserInfoService userInfoService;
    private final TokenValidator tokenValidator;

    /**
     * Refresh Token을 저장합니다.
     *
     * @param userId 사용자 ID
     * @param userType 사용자 타입
     * @param refreshToken 저장할 Refresh Token
     */
    public void storeRefreshToken(final Long userId, final String userType, final String refreshToken) {
        // 토큰 개수 제한 확인 및 강제 정리
        if (refreshTokenStorage.isRefreshTokenLimitExceeded(userType, userId)) {
            log.warn("Refresh token limit exceeded for user: {} (type: {}), forcing cleanup", userId, userType);
            refreshTokenStorage.forceCleanupOldTokens(userType, userId);
        }
        
        refreshTokenStorage.storeRefreshToken(userId, userType, refreshToken);
    }



    /**
     * Access Token을 갱신합니다. (RTR 방식)
     *
     * @param refreshToken 갱신에 사용할 Refresh Token
     * @return 새로운 토큰 정보가 포함된 응답
     * @throws BusinessException 토큰이 유효하지 않은 경우
     */
    public SocialLoginResponse refreshAccessToken(final String refreshToken) {
        // 1. TokenValidator를 사용하여 Refresh Token 검증 및 정보 추출
        final TokenValidator.TokenValidationResult validationResult = tokenValidator.validateRefreshToken(refreshToken);

        // 2. Refresh Token 개수 제한 확인
        validateRefreshTokenLimit(validationResult.userType(), validationResult.userId());

        // 3. 사용자 정보 조회
        final UserInfoService.UserInfo userInfo = userInfoService.getUserInfo(validationResult.userId(), validationResult.userType());

        // 4. RTR 방식: 기존 토큰을 즉시 무효화 (보안 강화)
        try {
            // 기존 토큰을 블랙리스트에 추가
            tokenBlacklistStorage.blacklistRefreshToken(refreshToken);
            // 기존 토큰을 Redis에서 삭제
            refreshTokenStorage.deleteRefreshToken(validationResult.userType(), validationResult.userId(), validationResult.tokenId());
            log.debug("RTR: Previous refresh token invalidated for user: {} (type: {})", validationResult.userId(), validationResult.userType());
        } catch (final BusinessException e) {
            log.error("Failed to invalidate previous refresh token during RTR for user: {} (type: {}): {}", 
                    validationResult.userId(), validationResult.userType(), e.getMessage());
            // RTR 방식에서는 무효화 실패 시 새 토큰 발급을 중단
            throw new BusinessException(ErrorCode.INVALID_JWT_TOKEN, "기존 토큰 무효화에 실패했습니다. 보안상 토큰 갱신을 중단합니다.");
        } catch (final Exception e) {
            log.error("Unexpected error during token invalidation for user: {} (type: {}): {}", 
                    validationResult.userId(), validationResult.userType(), e.getMessage());
            // 예상치 못한 오류도 토큰 갱신 중단
            throw new BusinessException(ErrorCode.INVALID_JWT_TOKEN, "토큰 무효화 중 오류가 발생했습니다.");
        }

        // 5. 새로운 토큰 생성
        final String newAccessToken = accessTokenGenerator.generateAccessToken(
                validationResult.userId(), userInfo.email(), UserRole.valueOf(userInfo.role()), UserType.fromString(validationResult.userType())
        );
        final String newRefreshToken = refreshTokenGenerator.generateRefreshToken(validationResult.userId(), UserType.fromString(validationResult.userType()));

        // 6. 새로운 Refresh Token 저장
        storeRefreshToken(validationResult.userId(), validationResult.userType(), newRefreshToken);

        log.info("RTR: Token refresh completed successfully for user: {} (type: {})", validationResult.userId(), validationResult.userType());

        return SocialLoginResponse.of(
                newAccessToken,
                newRefreshToken,
                accessTokenGenerator.getExpirySeconds(),
                SocialLoginResponse.MemberInfo.of(validationResult.userId(), userInfo.email(), null, null, validationResult.userType())
        );
    }

    /**
     * 로그아웃 처리를 합니다.
     *
     * @param authorization Authorization 헤더 값
     */
    public void logout(final String authorization) {
        final String accessToken = extractTokenFromBearer(authorization);
        logoutWithToken(accessToken);
    }

    /**
     * 토큰으로 로그아웃 처리를 합니다.
     *
     * @param accessToken 액세스 토큰
     */
    public void logoutWithToken(final String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            log.warn("Logout attempted with null or blank access token");
            return;
        }

        try {
            // TokenValidator를 사용하여 토큰 검증 및 사용자 정보 추출
            final TokenValidator.TokenValidationResult validationResult = tokenValidator.validateAccessToken(accessToken);

            // Refresh Token 삭제
            refreshTokenStorage.deleteAllRefreshTokens(validationResult.userType(), validationResult.userId());

            // Access Token 블랙리스트 처리
            try {
                tokenBlacklistStorage.blacklistAccessToken(accessToken);
                log.info("Logout successful for user: {} (type: {})", validationResult.userId(), validationResult.userType());
            } catch (final BusinessException e) {
                log.warn("Failed to blacklist access token during logout: {}", e.getMessage());
                // 블랙리스트 실패는 로그아웃을 중단시키지 않음
            }
        } catch (final Exception e) {
            log.warn("Logout failed due to invalid token: {}", e.getMessage());
            // 토큰이 유효하지 않아도 로그아웃은 성공으로 처리
        }
    }

    /**
     * 토큰이 블랙리스트에 있는지 확인합니다.
     *
     * @param tokenId 확인할 토큰 ID
     * @return 블랙리스트에 있으면 true
     */
    public boolean isTokenBlacklisted(final String tokenId) {
        return tokenBlacklistStorage.isTokenBlacklisted(tokenId);
    }

    // Private Helper Methods

    // validateRefreshToken 메서드 제거 - TokenValidator로 대체됨

    private void validateRefreshTokenLimit(final String userType, final Long userId) {
        // 토큰 개수 제한 확인
        if (refreshTokenStorage.isRefreshTokenLimitExceeded(userType, userId)) {
            log.warn("Refresh token 개수 제한 초과. userId: {}, userType: {}", userId, userType);
            
            // 강제 정리 시도
            try {
                refreshTokenStorage.forceCleanupOldTokens(userType, userId);
                log.info("Forced cleanup completed for user: {} (type: {})", userId, userType);
                
                // 정리 후 다시 확인
                if (refreshTokenStorage.isRefreshTokenLimitExceeded(userType, userId)) {
                    log.error("Refresh token limit still exceeded after cleanup for user: {} (type: {})", userId, userType);
                    throw new BusinessException(ErrorCode.TOO_MANY_REFRESH_TOKENS);
                }
            } catch (final Exception e) {
                log.error("Failed to cleanup old tokens for user: {} (type: {}): {}", userId, userType, e.getMessage());
                throw new BusinessException(ErrorCode.TOO_MANY_REFRESH_TOKENS);
            }
        }
    }

    private String extractTokenFromBearer(final String bearerToken) {
        try {
            return TokenExtractor.extractTokenFromBearer(bearerToken);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.MISSING_AUTHORIZATION_HEADER);
        }
    }
}