package com.ururulab.ururu.auth.jwt;

import com.ururulab.ururu.auth.constants.AuthConstants;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * JWT 토큰을 안전한 쿠키로 설정하는 헬퍼 클래스.
 *
 * 환경별 도메인 설정:
 * - 로컬 환경: 도메인 설정 없음 (localhost 호환)
 * - 운영환경: app.cookie.domain 프로퍼티 값 사용
 *
 * SameSite 정책:
 * - 로컬 환경: Lax (관대한 정책)
 * - 운영환경: None (크로스 사이트 허용, HTTPS 필수)
 */
@Slf4j
@Component
public final class JwtCookieHelper {

    private final JwtProperties jwtProperties;
    private final String cookieDomain;
    private final boolean secure;
    private final String sameSite;

    public JwtCookieHelper(
            final JwtProperties jwtProperties,
            @Value("${app.cookie.domain:}") final String cookieDomain
    ) {
        this.jwtProperties = jwtProperties;
        this.cookieDomain = cookieDomain.isBlank() ? null : cookieDomain.trim();
        this.secure = this.cookieDomain != null;
        this.sameSite = secure ? "None" : "Lax";
    }

    public void setAccessTokenCookie(final HttpServletResponse response, final String accessToken) {
        final long maxAgeSeconds = jwtProperties.getAccessTokenExpiry();
        addCookie(response, AuthConstants.ACCESS_TOKEN_COOKIE_NAME, accessToken, maxAgeSeconds);

        log.debug("Access token cookie set with expiry: {} seconds for domain: {}",
                maxAgeSeconds, cookieDomain);
    }

    public void setRefreshTokenCookie(final HttpServletResponse response, final String refreshToken) {
        final long maxAgeSeconds = jwtProperties.getRefreshTokenExpiry();
        addCookie(response, AuthConstants.REFRESH_TOKEN_COOKIE_NAME, refreshToken, maxAgeSeconds);

        log.debug("Refresh token cookie set with expiry: {} seconds for domain: {}",
                maxAgeSeconds, cookieDomain);
    }

    public void clearTokenCookies(final HttpServletResponse response) {
        addCookie(response, AuthConstants.ACCESS_TOKEN_COOKIE_NAME, "", 0);
        addCookie(response, AuthConstants.REFRESH_TOKEN_COOKIE_NAME, "", 0);

        log.info("All JWT token cookies cleared for domain: {}", cookieDomain);
    }

    private void addCookie(final HttpServletResponse response, final String name,
                           final String value, final long maxAgeSeconds) {
        final ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .sameSite(sameSite);

        if (cookieDomain != null) {
            builder.domain(cookieDomain);
        }

        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }
}
