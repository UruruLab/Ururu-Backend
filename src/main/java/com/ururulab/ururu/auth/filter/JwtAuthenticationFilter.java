package com.ururulab.ururu.auth.filter;

import com.ururulab.ururu.auth.constants.AuthConstants;
import com.ururulab.ururu.auth.service.TokenValidator;
import com.ururulab.ururu.auth.util.TokenExtractor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public final class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final TokenValidator tokenValidator;

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain
    ) throws ServletException, IOException {

        final String token = extractToken(request);

        if (token != null) {
            try {
                // TokenValidator를 사용하여 토큰 검증 및 사용자 정보 추출
                final TokenValidator.TokenValidationResult validationResult = tokenValidator.validateAccessToken(token);
                setAuthentication(validationResult);
            } catch (final Exception e) {
                log.debug("Token validation failed in filter: {}", e.getMessage());
                // 검증 실패 시 인증 컨텍스트 클리어
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(final HttpServletRequest request) {
        // API 클라이언트가 명시한 Authorization 헤더를 우선합니다.
        final String headerToken = extractTokenFromHeader(request);
        if (headerToken != null) {
            return headerToken;
        }
        return extractTokenFromCookie(request);
    }

    private String extractTokenFromCookie(final HttpServletRequest request) {
        final Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (final Cookie cookie : cookies) {
            if (AuthConstants.ACCESS_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                final String token = cookie.getValue();
                log.debug("쿠키에서 JWT 토큰 추출 성공");
                return token;
            }
        }
        return null;
    }

    private String extractTokenFromHeader(final HttpServletRequest request) {
        final String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (TokenExtractor.isValidAuthorizationHeader(authorizationHeader)) {
            final String token = TokenExtractor.extractTokenFromBearer(authorizationHeader);
            log.debug("Authorization 헤더에서 JWT 토큰 추출 성공");
            return token;
        }
        return null;
    }

    private void setAuthentication(final TokenValidator.TokenValidationResult validationResult) {
        try {
            final Long userId = validationResult.userId();
            
            final List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + validationResult.role())
            );

            final Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    authorities
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("JWT 토큰으로 인증 완료 - userId: {}, userType: {}, authority: {}",
                    userId, validationResult.userType(), authorities);

        } catch (final Exception e) {
            log.error("인증 처리 중 오류 발생: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }
    }

    @Override
    protected boolean shouldNotFilter(final HttpServletRequest request) {
        final String path = request.getRequestURI();

        // 인증이 필요 없는 경로들
        return path.startsWith("/api/auth/") ||
               path.startsWith("/api/public/") ||
               path.equals("/api/sellers/signup") ||  
               path.startsWith("/api/sellers/check/") ||  
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/v3/api-docs/");
    }
}
