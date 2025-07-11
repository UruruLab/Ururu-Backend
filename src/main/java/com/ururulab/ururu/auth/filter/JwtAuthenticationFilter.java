package com.ururulab.ururu.auth.filter;

import com.ururulab.ururu.auth.constants.AuthConstants;
import com.ururulab.ururu.auth.constants.UserType;
import com.ururulab.ururu.auth.jwt.JwtTokenProvider;
import com.ururulab.ururu.auth.service.TokenValidator;
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

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final JwtTokenProvider jwtTokenProvider;
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

        if (authorizationHeader != null && authorizationHeader.startsWith(AuthConstants.BEARER_PREFIX)) {
            final String token = authorizationHeader.substring(AuthConstants.BEARER_PREFIX.length());
            log.debug("Authorization 헤더에서 JWT 토큰 추출 성공");
            return token;
        }
        return null;
    }

    private void setAuthentication(final TokenValidator.TokenValidationResult validationResult) {
        try {
            final String userType = validationResult.userType();
            final Long userId = validationResult.userId();
            
            // userType이 null이거나 없는 경우 기본값으로 MEMBER 사용
            final String actualUserType = (userType != null && !userType.isBlank()) ? userType : AuthConstants.DEFAULT_USER_TYPE.getValue();
            
            // userType에 따라 authority 결정
            final String authority;
            
            if (UserType.SELLER.getValue().equals(actualUserType)) {
                authority = AuthConstants.AUTHORITY_ROLE_SELLER; // 판매자 전용 권한
                log.debug("판매자 인증 처리 - sellerId: {}", userId);
            } else {
                authority = AuthConstants.AUTHORITY_ROLE_MEMBER; // 회원 전용 권한
                log.debug("회원 인증 처리 - memberId: {}", userId);
            }

            final Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority(authority))
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("JWT 토큰으로 인증 완료 - userId: {}, userType: {}, authority: {}",
                    userId, actualUserType, authority);

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
               path.equals("/health") ||
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/v3/api-docs/");
    }
}
