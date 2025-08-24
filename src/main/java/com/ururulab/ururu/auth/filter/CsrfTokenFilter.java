package com.ururulab.ururu.auth.filter;

import com.ururulab.ururu.auth.service.CsrfTokenService;
import com.ururulab.ururu.auth.service.TokenValidator;
import com.ururulab.ururu.global.exception.BusinessException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * CSRF 토큰 검증 필터.
 * 
 * JWT 쿠키 기반 인증에서 CSRF 공격을 방지하기 위해 CSRF 토큰을 검증합니다.
 * 수정 요청(POST, PUT, DELETE, PATCH)에 대해서만 CSRF 토큰을 검증합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Lazy  // 순환 참조 방지
public final class CsrfTokenFilter extends OncePerRequestFilter {

    private static final String CSRF_TOKEN_HEADER = "X-CSRF-TOKEN";
    private static final List<HttpMethod> CSRF_PROTECTED_METHODS = Arrays.asList(
            HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.PATCH
    );

    private final CsrfTokenService csrfTokenService;
    private final TokenValidator tokenValidator;

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain
    ) throws ServletException, IOException {

        final String method = request.getMethod();
        final HttpMethod httpMethod = HttpMethod.valueOf(method);

        // CSRF 보호가 필요한 메서드인지 확인
        if (!CSRF_PROTECTED_METHODS.contains(httpMethod)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 인증이 필요 없는 경로인지 확인
        if (shouldSkipCsrfValidation(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // JWT 토큰에서 사용자 정보 추출
            final String token = extractTokenFromRequest(request);
            if (token == null) {
                log.debug("JWT 토큰이 없어 CSRF 검증을 건너뜁니다");
                filterChain.doFilter(request, response);
                return;
            }

            final TokenValidator.TokenValidationResult validationResult = tokenValidator.validateAccessToken(token);
            
            // CSRF 토큰 검증
            final String csrfToken = request.getHeader(CSRF_TOKEN_HEADER);
            csrfTokenService.validateCsrfToken(validationResult.userId(), validationResult.userType(), csrfToken);
            
            log.debug("CSRF 토큰 검증 성공 - userId: {}, userType: {}, method: {}", 
                    validationResult.userId(), validationResult.userType(), method);
            
        } catch (final BusinessException e) {
            log.warn("CSRF 토큰 검증 실패: {} - {}", e.getErrorCode().getCode(), e.getMessage());
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.getWriter().write("CSRF 토큰 검증에 실패했습니다.");
            return;
        } catch (final Exception e) {
            log.debug("CSRF 검증 중 예외 발생: {}", e.getMessage());
            // 예외 발생 시 요청을 계속 진행 (fail-open)
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 요청에서 JWT 토큰을 추출합니다.
     */
    private String extractTokenFromRequest(final HttpServletRequest request) {
        // 쿠키에서 토큰 추출
        final jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (final jakarta.servlet.http.Cookie cookie : cookies) {
                if ("access_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        // Authorization 헤더에서 토큰 추출
        final String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }

        return null;
    }

    /**
     * CSRF 검증을 건너뛸 경로인지 확인합니다.
     */
    private boolean shouldSkipCsrfValidation(final HttpServletRequest request) {
        final String path = request.getRequestURI();
        
        return path.startsWith("/api/auth/") ||
               path.startsWith("/api/public/") ||
               path.equals("/health") ||
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/v3/api-docs/") ||
               path.startsWith("/h2-console/") ||
               path.startsWith("/actuator/");
    }

    @Override
    protected boolean shouldNotFilter(final HttpServletRequest request) {
        final String path = request.getRequestURI();
        
        // 정적 리소스는 CSRF 검증 제외
        return path.startsWith("/static/") ||
               path.startsWith("/css/") ||
               path.startsWith("/js/") ||
               path.startsWith("/images/") ||
               path.endsWith(".ico") ||
               path.endsWith(".css") ||
               path.endsWith(".js");
    }
}
