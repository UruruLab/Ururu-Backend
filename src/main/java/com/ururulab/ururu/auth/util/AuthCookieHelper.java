package com.ururulab.ururu.auth.util;

import com.ururulab.ururu.auth.dto.response.SocialLoginResponse;
import com.ururulab.ururu.auth.jwt.JwtCookieHelper;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 인증 쿠키 설정을 위한 공통 헬퍼 클래스.
 * JWT 토큰을 안전한 쿠키로 설정하는 로직을 중앙화합니다.
 */
public final class AuthCookieHelper {

    private AuthCookieHelper() {
        // 유틸리티 클래스이므로 인스턴스화 방지
    }

    /**
     * JWT 토큰을 안전한 쿠키로 설정.
     *
     * @param response HTTP 응답 객체
     * @param loginResponse 로그인 응답
     * @param jwtCookieHelper JWT 쿠키 헬퍼
     */
    public static void setSecureCookies(
            final HttpServletResponse response,
            final SocialLoginResponse loginResponse,
            final JwtCookieHelper jwtCookieHelper
    ) {
        jwtCookieHelper.setAccessTokenCookie(response, loginResponse.accessToken());
        
        if (loginResponse.refreshToken() != null) {
            jwtCookieHelper.setRefreshTokenCookie(response, loginResponse.refreshToken());
        }
    }
} 