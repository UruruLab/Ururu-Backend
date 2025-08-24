package com.ururulab.ururu.auth.jwt;

import com.ururulab.ururu.auth.constants.AuthConstants;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.ururulab.ururu.auth.util.EnvironmentHelper;

/**
 * JWT 토큰을 안전한 쿠키로 설정하는 헬퍼 클래스.
 *
 * 환경별 도메인 설정:
 * - 개발환경: 도메인 설정 없음 (localhost 호환)
 * - 운영환경: app.cookie.domain 프로퍼티 값 사용
 *
 * SameSite 정책:
 * - 개발환경: Lax (관대한 정책)
 * - 운영환경: None (크로스 사이트 허용, HTTPS 필수)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public final class JwtCookieHelper {

    private static final String COOKIE_PATH = "/";

    private static final String SAME_SITE_DEV = "Lax";
    private static final String SAME_SITE_PROD = "None";

    private final JwtProperties jwtProperties;
    private final EnvironmentHelper environmentHelper;

    public void setAccessTokenCookie(final HttpServletResponse response, final String accessToken) {
        final long maxAgeSeconds = jwtProperties.getAccessTokenExpiry();
        setCookie(response, AuthConstants.ACCESS_TOKEN_COOKIE_NAME, accessToken, (int) maxAgeSeconds);

        log.debug("Access token cookie set with expiry: {} seconds for domain: {}",
                maxAgeSeconds, getCookieDomain());
    }

    public void setRefreshTokenCookie(final HttpServletResponse response, final String refreshToken) {
        final long maxAgeSeconds = jwtProperties.getRefreshTokenExpiry();
        setCookie(response, AuthConstants.REFRESH_TOKEN_COOKIE_NAME, refreshToken, (int) maxAgeSeconds);

        log.debug("Refresh token cookie set with expiry: {} seconds for domain: {}",
                maxAgeSeconds, getCookieDomain());
    }

    public void clearTokenCookies(final HttpServletResponse response) {
        clearCookie(response, AuthConstants.ACCESS_TOKEN_COOKIE_NAME);
        clearCookie(response, AuthConstants.REFRESH_TOKEN_COOKIE_NAME);

        log.info("All JWT token cookies cleared for domain: {}", getCookieDomain());
    }

    private void setCookie(final HttpServletResponse response, final String name,
                           final String value, final int maxAgeSeconds) {
        final String domain = getCookieDomain();

        // Set-Cookie 헤더만 사용 (중복 방지)
        final String setCookieHeader = buildSetCookieHeader(name, value, maxAgeSeconds, domain);
        response.addHeader("Set-Cookie", setCookieHeader);

        log.debug("Cookie set: {} for domain: {} with SameSite: {}",
                name, domain, getSameSitePolicy());
    }

    private void clearCookie(final HttpServletResponse response, final String name) {
        final String domain = getCookieDomain();

        // Set-Cookie 헤더만 사용 (중복 방지)
        final String setCookieHeader = buildClearCookieHeader(name, domain);
        response.addHeader("Set-Cookie", setCookieHeader);
    }

    /**
     * Set-Cookie 헤더 생성 (쿠키 설정용)
     */
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

    /**
     * Set-Cookie 헤더 생성 (쿠키 삭제용)
     */
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
     * 환경별 도메인 설정
     *
     * @return 개발환경: null, 운영환경: app.cookie.domain 프로퍼티 값
     */
    private String getCookieDomain() {
        if (environmentHelper.isDevelopmentEnvironment()) {
            return null; // 개발환경에서는 도메인 설정 없음
        }

        try {
            // 운영환경에서는 프로퍼티에서 가져옴 (필수값)
            final String cookieDomain = environmentHelper.getEnvironment().getProperty("app.cookie.domain");
            if (cookieDomain == null || cookieDomain.trim().isEmpty()) {
                throw new IllegalStateException("운영환경에서 app.cookie.domain 프로퍼티는 필수입니다");
            }
            return cookieDomain.trim();
        } catch (Exception e) {
            log.error("Cookie domain property access failed: {}", e.getMessage());
            throw new IllegalStateException("운영환경에서 app.cookie.domain 프로퍼티 접근 실패", e);
        }
    }

    /**
     * 환경별 SameSite 정책
     */
    private String getSameSitePolicy() {
        return environmentHelper.isDevelopmentEnvironment() ? SAME_SITE_DEV : SAME_SITE_PROD;
    }

    private boolean isSecureEnvironment() {
        return !environmentHelper.isDevelopmentEnvironment();
    }
}