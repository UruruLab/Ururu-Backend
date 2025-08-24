package com.ururulab.ururu.auth.filter;

import com.ururulab.ururu.auth.constants.AuthConstants;
import com.ururulab.ururu.auth.constants.UserType;
import com.ururulab.ururu.auth.service.TokenValidator;
import com.ururulab.ururu.global.exception.BusinessException;
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

/**
 * JWT 인증 필터.
 * 
 * <p>HTTP 요청에서 JWT 토큰을 추출하고 검증하여 Spring Security 인증 컨텍스트를 설정합니다.
 * 쿠키와 Authorization 헤더에서 토큰을 추출하며, 토큰 검증 실패 시 인증 컨텍스트를 클리어합니다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Lazy  // 순환 참조 방지
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
                final TokenValidator.TokenValidationResult validationResult = tokenValidator.validateAccessToken(token);
                setAuthentication(validationResult);
            } catch (final BusinessException e) {
                log.debug("Token validation failed in filter: {} - {}", e.getErrorCode().getCode(), e.getMessage());
                SecurityContextHolder.clearContext();
            } catch (final Exception e) {
                log.debug("Unexpected token validation error in filter: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    // ==================== Private Helper Methods ====================

    /**
     * 요청에서 JWT 토큰을 추출합니다.
     * 우선순위: 쿠키 > Authorization 헤더
     */
    private String extractToken(final HttpServletRequest request) {
        final String cookieToken = extractTokenFromCookie(request);
        if (cookieToken != null) {
            return cookieToken;
        }

        return extractTokenFromHeader(request);
    }

    /**
     * 쿠키에서 JWT 토큰을 추출합니다.
     */
    private String extractTokenFromCookie(final HttpServletRequest request) {
        final Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (final Cookie cookie : cookies) {
            if (AuthConstants.ACCESS_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                final String token = cookie.getValue();
                log.debug("JWT 토큰을 쿠키에서 추출했습니다");
                return token;
            }
        }
        return null;
    }

    /**
     * Authorization 헤더에서 JWT 토큰을 추출합니다.
     */
    private String extractTokenFromHeader(final HttpServletRequest request) {
        final String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authorizationHeader != null && authorizationHeader.startsWith(AuthConstants.BEARER_PREFIX)) {
            final String token = authorizationHeader.substring(AuthConstants.BEARER_PREFIX.length());
            log.debug("JWT 토큰을 Authorization 헤더에서 추출했습니다");
            return token;
        }
        return null;
    }

    /**
     * 토큰 검증 결과를 기반으로 Spring Security 인증을 설정합니다.
     */
    private void setAuthentication(final TokenValidator.TokenValidationResult validationResult) {
        try {
            final String userType = validationResult.userType();
            final Long userId = validationResult.userId();
            
            final String actualUserType = getValidUserType(userType);
            final String authority = determineAuthority(actualUserType);
            
            final Authentication authentication = createAuthentication(userId, authority);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            log.debug("JWT 토큰 인증 완료 - userId: {}, userType: {}, authority: {}",
                    userId, actualUserType, authority);

        } catch (final Exception e) {
            log.error("인증 처리 중 오류 발생: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * 유효한 사용자 타입을 반환합니다.
     */
    private String getValidUserType(final String userType) {
        return (userType != null && !userType.isBlank()) ? userType : UserType.MEMBER.getValue();
    }

    /**
     * 사용자 타입에 따른 권한을 결정합니다.
     */
    private String determineAuthority(final String userType) {
        if (UserType.SELLER.getValue().equals(userType)) {
            log.debug("판매자 인증 처리 - userType: {}", userType);
            return AuthConstants.AUTHORITY_ROLE_SELLER;
        } else {
            log.debug("회원 인증 처리 - userType: {}", userType);
            return AuthConstants.AUTHORITY_ROLE_MEMBER;
        }
    }

    /**
     * Spring Security 인증 객체를 생성합니다.
     */
    private Authentication createAuthentication(final Long userId, final String authority) {
        return new UsernamePasswordAuthenticationToken(
                userId,
                null,
                Collections.singletonList(new SimpleGrantedAuthority(authority))
        );
    }

    @Override
    protected boolean shouldNotFilter(final HttpServletRequest request) {
        final String path = request.getRequestURI();

        // 인증이 필요 없는 경로들
        return path.startsWith("/api/auth/") ||
               path.startsWith("/api/public/") ||
               path.equals("/api/sellers/signup") ||  
               path.startsWith("/api/sellers/check/") ||  
               path.equals("/health") ||
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/v3/api-docs/");
    }
}
