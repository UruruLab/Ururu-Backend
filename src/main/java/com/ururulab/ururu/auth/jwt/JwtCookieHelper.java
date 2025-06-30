package com.ururulab.ururu.auth.jwt;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * JWT 토큰을 안전한 쿠키로 설정하는 헬퍼 클래스.
 * 
 * XSS 공격 방지를 위한 HttpOnly, HTTPS 통신을 위한 Secure,
 * CSRF 공격 방지를 위한 SameSite 속성을 적용합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public final class JwtCookieHelper {

    private static final String ACCESS_TOKEN_COOKIE_NAME = "access_token";
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
    private static final String COOKIE_PATH = "/";
    private static final String SAME_SITE_STRICT = "Strict";
    
    private final JwtProperties jwtProperties;

    public void setAccessTokenCookie(final HttpServletResponse response, final String accessToken) {
        final long maxAgeSeconds = jwtProperties.getAccessTokenExpiry() / 1000;
        setCookie(response, ACCESS_TOKEN_COOKIE_NAME, accessToken, (int) maxAgeSeconds);
        
        log.info("Access token cookie set with expiry: {} seconds", maxAgeSeconds);
    }

    public void setRefreshTokenCookie(final HttpServletResponse response, final String refreshToken) {
        final long maxAgeSeconds = jwtProperties.getRefreshTokenExpiry() / 1000;
        setCookie(response, REFRESH_TOKEN_COOKIE_NAME, refreshToken, (int) maxAgeSeconds);
        
        log.info("Refresh token cookie set with expiry: {} seconds", maxAgeSeconds);
    }

    public void clearTokenCookies(final HttpServletResponse response) {
        clearCookie(response, ACCESS_TOKEN_COOKIE_NAME);
        clearCookie(response, REFRESH_TOKEN_COOKIE_NAME);
        
        log.info("All JWT token cookies cleared");
    }

    private void setCookie(final HttpServletResponse response, final String name, 
                          final String value, final int maxAgeSeconds) {
        final Cookie cookie = new Cookie(name, value);
        
        cookie.setHttpOnly(true);
        cookie.setSecure(isSecureEnvironment());
        cookie.setPath(COOKIE_PATH);
        cookie.setMaxAge(maxAgeSeconds);
        
        response.addCookie(cookie);
        response.addHeader("Set-Cookie", 
            String.format("%s=%s; Path=%s; Max-Age=%d; HttpOnly; %s; SameSite=%s",
                name, value, COOKIE_PATH, maxAgeSeconds,
                isSecureEnvironment() ? "Secure" : "",
                SAME_SITE_STRICT));
    }

    private void clearCookie(final HttpServletResponse response, final String name) {
        final Cookie cookie = new Cookie(name, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(isSecureEnvironment());
        cookie.setPath(COOKIE_PATH);
        cookie.setMaxAge(0);
        
        response.addCookie(cookie);
        response.addHeader("Set-Cookie", 
            String.format("%s=; Path=%s; Max-Age=0; HttpOnly; %s; SameSite=%s",
                name, COOKIE_PATH,
                isSecureEnvironment() ? "Secure" : "",
                SAME_SITE_STRICT));
    }

    private boolean isSecureEnvironment() {
        return !isDevelopmentProfile();
    }

    private boolean isDevelopmentProfile() {
        final String activeProfile = System.getProperty("spring.profiles.active", "dev");
        return "dev".equals(activeProfile) || "local".equals(activeProfile);
    }

    private String maskSensitiveData(final String data) {
        if (data == null || data.length() <= 10) {
            return "***";
        }
        return data.substring(0, 10) + "...";
    }
}