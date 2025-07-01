package com.ururulab.ururu.auth.jwt;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    // 환경별 SameSite 정책 분리
    private static final String SAME_SITE_DEV = "Lax";      // 개발환경: 관대한 정책
    private static final String SAME_SITE_PROD = "None";    // 운영환경: 크로스 사이트 허용 (HTTPS 필수)

    private final JwtProperties jwtProperties;

    @Value("${spring.profiles.active:prod}")
    private String activeProfile;

    // 도메인 설정을 위한 프로퍼티 (application.yml에서 설정)
    @Value("${app.cookie.domain:}")
    private String cookieDomain;

    public void setAccessTokenCookie(final HttpServletResponse response, final String accessToken) {
        final long maxAgeSeconds = jwtProperties.getAccessTokenExpiry() / 1000;
        setCookie(response, ACCESS_TOKEN_COOKIE_NAME, accessToken, (int) maxAgeSeconds);

        log.debug("Access token cookie set with expiry: {} seconds for domain: {}",
                maxAgeSeconds, getCookieDomain());
    }

    public void setRefreshTokenCookie(final HttpServletResponse response, final String refreshToken) {
        final long maxAgeSeconds = jwtProperties.getRefreshTokenExpiry() / 1000;
        setCookie(response, REFRESH_TOKEN_COOKIE_NAME, refreshToken, (int) maxAgeSeconds);

        log.debug("Refresh token cookie set with expiry: {} seconds for domain: {}",
                maxAgeSeconds, getCookieDomain());
    }

    public void clearTokenCookies(final HttpServletResponse response) {
        clearCookie(response, ACCESS_TOKEN_COOKIE_NAME);
        clearCookie(response, REFRESH_TOKEN_COOKIE_NAME);

        log.info("All JWT token cookies cleared for domain: {}", getCookieDomain());
    }

    private void setCookie(final HttpServletResponse response, final String name,
                           final String value, final int maxAgeSeconds) {
        final Cookie cookie = new Cookie(name, value);

        cookie.setHttpOnly(true);
        cookie.setSecure(isSecureEnvironment());
        cookie.setPath(COOKIE_PATH);
        cookie.setMaxAge(maxAgeSeconds);

        // 도메인 설정 (운영환경에서만)
        final String domain = getCookieDomain();
        if (domain != null && !domain.isEmpty()) {
            cookie.setDomain(domain);
        }

        response.addCookie(cookie);

        // Set-Cookie 헤더에 SameSite 속성 추가
        final String setCookieHeader = buildSetCookieHeader(name, value, maxAgeSeconds, domain);
        response.addHeader("Set-Cookie", setCookieHeader);

        log.debug("Cookie set: {} for domain: {} with SameSite: {}",
                name, domain, getSameSitePolicy());
    }

    private void clearCookie(final HttpServletResponse response, final String name) {
        final Cookie cookie = new Cookie(name, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(isSecureEnvironment());
        cookie.setPath(COOKIE_PATH);
        cookie.setMaxAge(0);

        // 도메인 설정 (쿠키 삭제 시에도 동일한 도메인 필요)
        final String domain = getCookieDomain();
        if (domain != null && !domain.isEmpty()) {
            cookie.setDomain(domain);
        }

        response.addCookie(cookie);

        // Set-Cookie 헤더에 SameSite 속성 추가
        final String setCookieHeader = buildClearCookieHeader(name, domain);
        response.addHeader("Set-Cookie", setCookieHeader);
    }

    private String buildSetCookieHeader(final String name, final String value,
                                        final int maxAgeSeconds, final String domain) {
        final StringBuilder builder = new StringBuilder();
        builder.append(String.format("%s=%s; Path=%s; Max-Age=%d; HttpOnly",
                name, value, COOKIE_PATH, maxAgeSeconds));

        if (isSecureEnvironment()) {
            builder.append("; Secure");
        }

        if (domain != null && !domain.isEmpty()) {
            builder.append("; Domain=").append(domain);
        }

        builder.append("; SameSite=").append(getSameSitePolicy());

        return builder.toString();
    }

    private String buildClearCookieHeader(final String name, final String domain) {
        final StringBuilder builder = new StringBuilder();
        builder.append(String.format("%s=; Path=%s; Max-Age=0; HttpOnly", name, COOKIE_PATH));

        if (isSecureEnvironment()) {
            builder.append("; Secure");
        }

        if (domain != null && !domain.isEmpty()) {
            builder.append("; Domain=").append(domain);
        }

        builder.append("; SameSite=").append(getSameSitePolicy());

        return builder.toString();
    }

    /**
     * 환경별 도메인 설정 반환
     */
    private String getCookieDomain() {
        if (isDevelopmentProfile()) {
            return null; // 개발환경에서는 도메인 설정 없음 (localhost)
        }

        // 운영환경에서는 설정값 사용하거나 기본값
        return cookieDomain.isEmpty() ? ".o-r.kr" : cookieDomain;
    }

    /**
     * 환경별 SameSite 정책 반환
     */
    private String getSameSitePolicy() {
        return isDevelopmentProfile() ? SAME_SITE_DEV : SAME_SITE_PROD;
    }

    private boolean isSecureEnvironment() {
        return !isDevelopmentProfile();
    }

    private boolean isDevelopmentProfile() {
        return "dev".equals(activeProfile);
    }

    private String maskSensitiveData(final String data) {
        if (data == null || data.length() <= 10) {
            return "***";
        }
        return data.substring(0, 10) + "...";
    }
}