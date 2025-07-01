package com.ururulab.ururu.auth.jwt;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public final class JwtCookieHelper {

    private static final String ACCESS_TOKEN_COOKIE_NAME = "access_token";
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
    private static final String COOKIE_PATH = "/";

    private static final String SAME_SITE_DEV = "Lax";
    private static final String SAME_SITE_PROD = "None";

    private final JwtProperties jwtProperties;
    private final Environment environment;

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

        final String domain = getCookieDomain();
        if (domain != null && !domain.isEmpty()) {
            cookie.setDomain(domain);
        }

        response.addCookie(cookie);

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

        final String domain = getCookieDomain();
        if (domain != null && !domain.isEmpty()) {
            cookie.setDomain(domain);
        }

        response.addCookie(cookie);

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
     * 안전한 프로파일 확인 (null-safe)
     */
    private boolean isDevelopmentProfile() {
        try {
            return environment.acceptsProfiles("dev");
        } catch (Exception e) {
            // 테스트 환경이나 프로파일이 설정되지 않은 경우 개발환경으로 간주
            log.debug("Profile check failed, defaulting to development: {}", e.getMessage());
            return true;
        }
    }

    /**
     * 환경별 도메인 설정 (null-safe)
     */
    private String getCookieDomain() {
        if (isDevelopmentProfile()) {
            return null; // 개발환경에서는 도메인 설정 없음
        }

        try {
            // 운영환경에서는 프로퍼티에서 가져오거나 기본값
            return environment.getProperty("app.cookie.domain", ".o-r.kr");
        } catch (Exception e) {
            log.debug("Cookie domain property access failed, using default: {}", e.getMessage());
            return ".o-r.kr";
        }
    }

    /**
     * 환경별 SameSite 정책
     */
    private String getSameSitePolicy() {
        return isDevelopmentProfile() ? SAME_SITE_DEV : SAME_SITE_PROD;
    }

    private boolean isSecureEnvironment() {
        return !isDevelopmentProfile();
    }
}
