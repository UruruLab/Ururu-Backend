package com.ururulab.ururu.auth.service;

import com.ururulab.ururu.auth.constants.AuthConstants;
import com.ururulab.ururu.auth.constants.UserRole;
import com.ururulab.ururu.auth.constants.UserType;
import com.ururulab.ururu.auth.dto.response.SocialLoginResponse;
import com.ururulab.ururu.auth.jwt.JwtProperties;
import com.ururulab.ururu.auth.jwt.JwtTokenProvider;
import com.ururulab.ururu.auth.jwt.token.AccessTokenGenerator;
import com.ururulab.ururu.auth.jwt.token.RefreshTokenGenerator;
import com.ururulab.ururu.auth.jwt.validator.JwtTokenValidator;
import com.ururulab.ururu.auth.storage.RefreshTokenStorage;
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
    private final JwtTokenValidator jwtTokenValidator;
    private final RefreshTokenStorage refreshTokenStorage;
    private final TokenBlacklistStorage tokenBlacklistStorage;
    private final UserInfoService userInfoService;

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
        // 1. Refresh Token 검증 (JWT 서명 + 만료시간)
        validateRefreshToken(refreshToken);

        // 2. 토큰에서 정보 추출
        final Long userId = jwtTokenProvider.getMemberId(refreshToken);
        final String userType = jwtTokenProvider.getUserType(refreshToken);
        final String tokenId = jwtTokenProvider.getTokenId(refreshToken);

        // 3. 블랙리스트 확인
        if (tokenBlacklistStorage.isTokenBlacklisted(tokenId)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 4. Refresh Token 개수 제한 확인
        validateRefreshTokenLimit(userType, userId);

        // 5. 사용자 정보 조회
        final UserInfoService.UserInfo userInfo = userInfoService.getUserInfo(userId, userType);

        // 6. RTR 방식: 기존 토큰을 즉시 무효화 (보안 강화)
        try {
            // 기존 토큰을 블랙리스트에 추가
            tokenBlacklistStorage.blacklistRefreshToken(refreshToken);
            // 기존 토큰을 Redis에서 삭제
            refreshTokenStorage.deleteRefreshToken(userType, userId, tokenId);
            log.debug("RTR: Previous refresh token invalidated for user: {} (type: {})", userId, userType);
        } catch (final BusinessException e) {
            log.warn("Failed to invalidate previous refresh token during RTR: {}", e.getMessage());
            // 토큰 무효화 실패 시에도 갱신 계속 진행 (기존 토큰은 만료되면 자동 삭제됨)
        }

        // 7. 새로운 토큰 생성
        final String newAccessToken = accessTokenGenerator.generateAccessToken(
                userId, userInfo.email(), UserRole.valueOf(userInfo.role()), UserType.fromString(userType)
        );
        final String newRefreshToken = refreshTokenGenerator.generateRefreshToken(userId, UserType.fromString(userType));

        // 8. 새로운 Refresh Token 저장
        storeRefreshToken(userId, userType, newRefreshToken);

        log.info("RTR: Token refresh completed successfully for user: {} (type: {})", userId, userType);

        return SocialLoginResponse.of(
                newAccessToken,
                newRefreshToken,
                accessTokenGenerator.getExpirySeconds(),
                SocialLoginResponse.MemberInfo.of(userId, userInfo.email(), null, null, userType)
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
            final Long userId = jwtTokenProvider.getMemberId(accessToken);
            final String userType = jwtTokenProvider.getUserType(accessToken);

            // Refresh Token 삭제
            refreshTokenStorage.deleteAllRefreshTokens(userType, userId);

            // Access Token 블랙리스트 처리
            try {
                tokenBlacklistStorage.blacklistAccessToken(accessToken);
                log.info("Logout successful for user: {} (type: {})", userId, userType);
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

    private void validateRefreshToken(final String refreshToken) {
        jwtTokenValidator.validateRefreshToken(refreshToken);
    }

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
        if (bearerToken == null || !bearerToken.startsWith(AuthConstants.BEARER_PREFIX)) {
            throw new BusinessException(ErrorCode.MISSING_AUTHORIZATION_HEADER);
        }
        return bearerToken.substring(AuthConstants.BEARER_PREFIX.length());
    }
}