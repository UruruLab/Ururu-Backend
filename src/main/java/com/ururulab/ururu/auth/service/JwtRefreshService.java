package com.ururulab.ururu.auth.service;

import com.ururulab.ururu.auth.constants.UserRole;
import com.ururulab.ururu.auth.constants.UserType;
import com.ururulab.ururu.auth.dto.response.SocialLoginResponse;
import com.ururulab.ururu.auth.jwt.token.AccessTokenGenerator;
import com.ururulab.ururu.auth.jwt.token.RefreshTokenGenerator;
import com.ururulab.ururu.auth.storage.RefreshTokenStorage;
import com.ururulab.ururu.auth.storage.TokenBlacklistStorage;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public final class JwtRefreshService {

    private final AccessTokenGenerator accessTokenGenerator;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final RefreshTokenStorage refreshTokenStorage;
    private final TokenBlacklistStorage tokenBlacklistStorage;
    private final UserInfoService userInfoService;
    private final TokenValidator tokenValidator;

    public void storeRefreshToken(final Long userId, final String userType, final String refreshToken) {
        refreshTokenStorage.storeRefreshToken(userId, userType, refreshToken);
    }

    public SocialLoginResponse refreshAccessToken(final String refreshToken) {
        final TokenValidator.TokenValidationResult validation = tokenValidator.validateRefreshToken(refreshToken);
        final UserInfoService.UserInfo userInfo = userInfoService.getUserInfo(
                validation.userId(),
                validation.userType()
        );

        final UserType userType = UserType.fromString(validation.userType());
        final String newAccessToken = accessTokenGenerator.generateAccessToken(
                validation.userId(),
                userInfo.email(),
                UserRole.valueOf(userInfo.role()),
                userType
        );
        final String newRefreshToken = refreshTokenGenerator.generateRefreshToken(validation.userId(), userType);
        if (!refreshTokenStorage.rotateRefreshToken(
                validation.userType(), validation.userId(), refreshToken, newRefreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        log.info("Refresh token rotated for user: {} (type: {})", validation.userId(), validation.userType());
        return SocialLoginResponse.of(
                newAccessToken,
                newRefreshToken,
                accessTokenGenerator.getExpirySeconds(),
                SocialLoginResponse.MemberInfo.of(
                        validation.userId(),
                        userInfo.email(),
                        null,
                        null,
                        validation.userType()
                )
        );
    }

    public void logoutWithToken(final String accessToken) {
        final TokenValidator.TokenValidationResult validation = tokenValidator.validateAccessToken(accessToken);
        tokenBlacklistStorage.revokeUserTokens(validation.userId(), validation.userType());
        refreshTokenStorage.deleteAllRefreshTokens(validation.userType(), validation.userId());
        log.info("Logout completed for user: {} (type: {})", validation.userId(), validation.userType());
    }

    /** Access Token이 만료됐더라도 유효한 Refresh Token으로 전체 세션을 종료합니다. */
    public void logoutWithTokens(final String accessToken, final String refreshToken) {
        if (accessToken != null && !accessToken.isBlank()) {
            try {
                logoutWithToken(accessToken);
                return;
            } catch (final BusinessException e) {
                if (e.getErrorCode() != ErrorCode.INVALID_JWT_TOKEN
                        && e.getErrorCode() != ErrorCode.EXPIRED_JWT_TOKEN
                        && e.getErrorCode() != ErrorCode.MALFORMED_JWT_TOKEN) {
                    throw e;
                }
            }
        }

        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        final TokenValidator.TokenValidationResult validation = tokenValidator.validateRefreshToken(refreshToken);
        tokenBlacklistStorage.revokeUserTokens(validation.userId(), validation.userType());
        refreshTokenStorage.deleteAllRefreshTokens(validation.userType(), validation.userId());
        log.info("Logout completed with refresh token for user: {} (type: {})",
                validation.userId(), validation.userType());
    }

    public void revokeAllTokens(final Long userId, final UserType userType) {
        tokenBlacklistStorage.revokeUserTokens(userId, userType.getValue());
        refreshTokenStorage.deleteAllRefreshTokens(userType.getValue(), userId);
    }

}
