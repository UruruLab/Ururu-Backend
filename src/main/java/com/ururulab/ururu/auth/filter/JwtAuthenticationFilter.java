package com.ururulab.ururu.auth.filter;

import com.ururulab.ururu.auth.jwt.JwtTokenProvider;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
@Lazy  // 순환 참조 방지
public final class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String ACCESS_TOKEN_COOKIE_NAME = "access_token";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain
    ) throws ServletException, IOException {

        final String token = extractToken(request);

        if (token != null && jwtTokenProvider.validateToken(token)) {
            setAuthentication(token);
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(final HttpServletRequest request) {
        // 1순위: 쿠키에서 토큰 추출
        final String cookieToken = extractTokenFromCookie(request);
        if (cookieToken != null) {
            return cookieToken;
        }

        // 2순위: Authorization 헤더에서 토큰 추출
        return extractTokenFromHeader(request);
    }

    private String extractTokenFromCookie(final HttpServletRequest request) {
        final Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (final Cookie cookie : cookies) {
            if (ACCESS_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                final String token = cookie.getValue();
                log.debug("쿠키에서 JWT 토큰 추출 성공");
                return token;
            }
        }
        return null;
    }

    private String extractTokenFromHeader(final HttpServletRequest request) {
        final String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
            final String token = authorizationHeader.substring(BEARER_PREFIX.length());
            log.debug("Authorization 헤더에서 JWT 토큰 추출 성공");
            return token;
        }
        return null;
    }

    private void setAuthentication(final String token) {
        try {
            final Long memberId = jwtTokenProvider.getMemberId(token);
            final String role = jwtTokenProvider.getRole(token);

            final Authentication authentication = new UsernamePasswordAuthenticationToken(
                    memberId,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("JWT 토큰으로 인증 완료 - memberId: {}, role: {}",
                    memberId, role);

        } catch (final JwtException e) { // 3. JwtException 구체적으로 처리
            log.warn("JWT 토큰 인증 처리 중 오류 발생: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        } catch (final Exception e) { // 4. 예상치 못한 일반 예외 처리
            log.error("예상치 못한 인증 오류 발생: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }
    }

    @Override
    protected boolean shouldNotFilter(final HttpServletRequest request) {
        final String path = request.getRequestURI();

        // 인증이 필요 없는 경로들
        return path.startsWith("/api/auth/") ||
               path.startsWith("/api/public/") ||
               path.equals("/health") ||
               path.equals("/actuator/health") ||
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/v3/api-docs/");
    }
}
