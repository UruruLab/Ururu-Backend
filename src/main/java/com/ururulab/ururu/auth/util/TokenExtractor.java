package com.ururulab.ururu.auth.util;

import com.ururulab.ururu.auth.constants.AuthConstants;
import lombok.extern.slf4j.Slf4j;

/**
 * 토큰 추출 유틸리티 클래스.
 * Authorization 헤더와 쿠키에서 토큰을 추출하는 로직을 통합 관리합니다.
 */
@Slf4j
public final class TokenExtractor {

    private TokenExtractor() {
        // 유틸리티 클래스이므로 인스턴스화 방지
    }

    /**
     * Authorization 헤더와 쿠키에서 토큰을 추출합니다.
     * 우선순위: Authorization 헤더 > 쿠키
     *
     * @param authorization Authorization 헤더 값
     * @param accessToken 쿠키의 액세스 토큰
     * @return 추출된 토큰 (Bearer 형식), 추출할 수 없으면 null
     */
    public static String extractTokenForLogout(final String authorization, final String accessToken) {
        // Authorization 헤더 우선 확인
        if (isValidAuthorizationHeader(authorization)) {
            log.debug("Token extracted from Authorization header");
            return authorization;
        }
        
        // 쿠키에서 액세스 토큰 확인
        if (isValidAccessToken(accessToken)) {
            log.debug("Token extracted from cookie");
            return AuthConstants.BEARER_PREFIX + accessToken;
        }
        
        log.debug("No valid token found in Authorization header or cookie");
        return null;
    }

    /**
     * Authorization 헤더가 유효한지 확인합니다.
     *
     * @param authorization Authorization 헤더 값
     * @return 유효하면 true
     */
    public static boolean isValidAuthorizationHeader(final String authorization) {
        return authorization != null && !authorization.isBlank() && authorization.startsWith(AuthConstants.BEARER_PREFIX);
    }

    /**
     * 액세스 토큰이 유효한지 확인합니다.
     *
     * @param accessToken 액세스 토큰
     * @return 유효하면 true
     */
    public static boolean isValidAccessToken(final String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return false;
        }
        
        // JWT 형식 검증 (3개의 base64 인코딩된 부분으로 구성)
        return accessToken.matches("^[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.?[A-Za-z0-9-_.+/=]*$");
    }

    /**
     * Bearer 토큰에서 실제 토큰 값을 추출합니다.
     *
     * @param bearerToken Bearer 형식의 토큰
     * @return 토큰 값
     * @throws IllegalArgumentException Bearer 형식이 아닌 경우
     */
    public static String extractTokenFromBearer(final String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith(AuthConstants.BEARER_PREFIX)) {
            throw new IllegalArgumentException("Invalid Bearer token format");
        }
        return bearerToken.substring(AuthConstants.BEARER_PREFIX.length());
    }

    /**
     * 토큰이 null이거나 비어있는지 검증합니다.
     *
     * @param token 검증할 토큰
     * @return 유효하면 true
     */
    public static boolean isValidToken(final String token) {
        return token != null && !token.isBlank();
    }

    /**
     * Refresh Token이 유효한지 검증합니다.
     *
     * @param refreshToken 검증할 Refresh Token
     * @return 유효하면 true
     */
    public static boolean isValidRefreshToken(final String refreshToken) {
        return isValidToken(refreshToken);
    }
} 