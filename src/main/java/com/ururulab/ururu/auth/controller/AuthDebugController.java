package com.ururulab.ururu.auth.controller;

import com.ururulab.ururu.auth.dto.response.SocialLoginResponse;
import com.ururulab.ururu.auth.jwt.JwtCookieHelper;
import com.ururulab.ururu.auth.service.JwtRefreshService;
import com.ururulab.ururu.global.domain.dto.ApiResponseFormat;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 인증 관련 디버그 API 컨트롤러.
 * 
 * <p>개발환경에서만 사용되는 디버그 기능을 제공합니다.
 * 운영환경에서는 모든 API가 차단되며, 보안을 위해 프로덕션 환경에서는 접근이 불가능합니다.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/auth/debug")
@RequiredArgsConstructor
public class AuthDebugController {

    private final JwtRefreshService jwtRefreshService;
    private final JwtCookieHelper jwtCookieHelper;
    private final Environment environment;

    /**
     * 소셜 로그인 설정 디버그 정보 API
     */
    @GetMapping("/social-config")
    public ResponseEntity<ApiResponseFormat<Object>> debugSocialConfig() {
        validateDevelopmentEnvironment();
        
        final Object debugInfo = createDebugInfo();
        
        return ResponseEntity.ok(
                ApiResponseFormat.success("소셜 로그인 설정 디버그 정보", debugInfo)
        );
    }

    /**
     * 토큰 갱신 API (디버그용 - 토큰 마스킹 없음).
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponseFormat<SocialLoginResponse>> refreshTokenDebug(
            @CookieValue(name = "refresh_token", required = false) final String refreshToken,
            final HttpServletResponse response) {
        
        validateDevelopmentEnvironment();
        
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.MISSING_REFRESH_TOKEN);
        }
        
        final SocialLoginResponse refreshResponse = jwtRefreshService.refreshAccessToken(refreshToken);
        
        // 새로운 토큰을 쿠키로 설정
        jwtCookieHelper.setAccessTokenCookie(response, refreshResponse.accessToken());
        if (refreshResponse.refreshToken() != null) {
            jwtCookieHelper.setRefreshTokenCookie(response, refreshResponse.refreshToken());
        }
        
        // 디버그용: 토큰을 마스킹하지 않고 그대로 반환
        log.info("Token refresh debug successful for user: {} (env: {})", 
                refreshResponse.memberInfo().email(), getCurrentProfile());
        
        return ResponseEntity.ok(
                ApiResponseFormat.success("토큰이 갱신되었습니다. (디버그 모드)", refreshResponse)
        );
    }

    // ==================== Private Helper Methods ====================

    /**
     * 개발환경인지 검증합니다.
     */
    private void validateDevelopmentEnvironment() {
        if (isProductionEnvironment()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    /**
     * 디버그 정보를 생성합니다.
     */
    private Object createDebugInfo() {
        return Map.of(
            "environment", getCurrentProfile(),
            "kakao", Map.of(
                "clientId", environment.getProperty("oauth2.kakao.client-id", "NOT_SET"),
                "redirectUri", environment.getProperty("oauth2.kakao.redirect-uri", "NOT_SET"),
                "authUri", environment.getProperty("oauth2.kakao.authorization-uri", "NOT_SET")
            ),
            "google", Map.of(
                "clientId", environment.getProperty("oauth2.google.client-id", "NOT_SET"),
                "redirectUri", environment.getProperty("oauth2.google.redirect-uri", "NOT_SET"),
                "authUri", environment.getProperty("oauth2.google.authorization-uri", "NOT_SET")
            ),
            "cors", Map.of(
                "allowedOrigins", environment.getProperty("app.cors.allowed-origins[0]", "NOT_SET"),
                "frontendUrl", getFrontendBaseUrl()
            )
        );
    }

    /**
     * 환경별 프론트엔드 기본 URL 결정.
     */
    private String getFrontendBaseUrl() {
        try {
            final String frontendUrl = environment.getProperty("app.frontend.base-url");
            if (frontendUrl != null && !frontendUrl.trim().isEmpty()) {
                return frontendUrl.trim();
            }
            
            return isProductionEnvironment() ? "https://www.ururu.shop" : "http://localhost:3000";
            
        } catch (final Exception e) {
            log.warn("Failed to get frontend URL from yml config, using fallback (env: {}): {}", 
                    getCurrentProfile(), e.getMessage());
            return isProductionEnvironment() ? "https://www.ururu.shop" : "http://localhost:3000";
        }
    }

    /**
     * 현재 환경이 운영환경인지 확인
     */
    private boolean isProductionEnvironment() {
        try {
            return environment.acceptsProfiles("prod");
        } catch (final Exception e) {
            log.error("Profile check failed, defaulting to production for safety: {}", e.getMessage());
            return true; // 안전을 위해 프로덕션으로 간주
        }
    }

    /**
     * 현재 활성 프로파일 반환 (로깅용)
     */
    private String getCurrentProfile() {
        try {
            final String[] activeProfiles = environment.getActiveProfiles();
            return activeProfiles.length > 0 ? String.join(",", activeProfiles) : "default";
        } catch (final Exception e) {
            return "unknown";
        }
    }
} 