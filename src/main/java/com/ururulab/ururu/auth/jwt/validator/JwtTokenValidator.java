package com.ururulab.ururu.auth.jwt.validator;

import com.ururulab.ururu.auth.jwt.JwtTokenProvider;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * JWT 토큰 검증을 전담하는 클래스.
 * 단일책임원칙에 따라 토큰 검증만 담당합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public final class JwtTokenValidator {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 토큰의 유효성을 검증합니다.
     *
     * @param token 검증할 JWT 토큰
     * @return 토큰이 유효하면 true, 그렇지 않으면 false
     */
    public boolean validateToken(final String token) {
        return jwtTokenProvider.validateToken(token);
    }

    /**
     * 토큰이 만료되었는지 확인합니다.
     *
     * @param token 확인할 JWT 토큰
     * @return 토큰이 만료되었으면 true, 그렇지 않으면 false
     */
    public boolean isTokenExpired(final String token) {
        return jwtTokenProvider.isTokenExpired(token);
    }

    /**
     * 토큰이 Refresh Token인지 확인합니다.
     *
     * @param token 확인할 JWT 토큰
     * @return Refresh Token이면 true, 그렇지 않으면 false
     */
    public boolean isRefreshToken(final String token) {
        return jwtTokenProvider.isRefreshToken(token);
    }

    /**
     * 토큰이 Access Token인지 확인합니다.
     *
     * @param token 확인할 JWT 토큰
     * @return Access Token이면 true, 그렇지 않으면 false
     */
    public boolean isAccessToken(final String token) {
        return jwtTokenProvider.isAccessToken(token);
    }

    /**
     * Refresh Token의 유효성을 검증합니다.
     *
     * @param token 검증할 Refresh Token
     * @throws BusinessException 토큰이 유효하지 않은 경우
     */
    public void validateRefreshToken(final String token) {
        if (!validateToken(token)) {
            log.warn("Invalid refresh token format");
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        
        if (!isRefreshToken(token)) {
            log.warn("Token is not a refresh token");
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    /**
     * Access Token의 유효성을 검증합니다.
     *
     * @param token 검증할 Access Token
     * @throws BusinessException 토큰이 유효하지 않은 경우
     */
    public void validateAccessToken(final String token) {
        if (!validateToken(token)) {
            log.warn("Invalid access token format");
            throw new BusinessException(ErrorCode.INVALID_JWT_TOKEN);
        }
        
        if (!isAccessToken(token)) {
            log.warn("Token is not an access token");
            throw new BusinessException(ErrorCode.INVALID_JWT_TOKEN);
        }
    }
} 