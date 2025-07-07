package com.ururulab.ururu.auth.controller;

import com.ururulab.ururu.auth.dto.response.SocialLoginResponse;
import com.ururulab.ururu.auth.jwt.JwtCookieHelper;
import com.ururulab.ururu.auth.service.SocialLoginServiceFactory;
import com.ururulab.ururu.auth.service.SocialLoginService;
import com.ururulab.ururu.global.domain.dto.ApiResponseFormat;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.member.domain.entity.enumerated.SocialProvider;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * 인증 관련 API 컨트롤러.
 * 
 * ALB + Route53 + https://www.ururu.shop 배포 환경 최적화:
 * - 쿠키 기반 JWT 토큰 관리 (URL 노출 없음)
 * - yml 설정 기반 환경별 자동 처리
 * - GlobalExceptionHandler 연동으로 간소화된 예외 처리
 * - OAuth 표준 플로우 준수
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final int SENSITIVE_DATA_PREVIEW_LENGTH = 8;
    private static final String MASKED_DATA_PLACEHOLDER = "***";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder BASE64_ENCODER = Base64.getUrlEncoder();

    // OAuth 코드 중복 사용 방지를 위한 캐시 (개발환경용 임시 저장소)
    private static final java.util.Set<String> USED_CODES = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private final SocialLoginServiceFactory socialLoginServiceFactory;
    private final JwtCookieHelper jwtCookieHelper;
    private final Environment environment;

    /**
     * 카카오 OAuth 콜백 처리.
     * 
     * OAuth 표준 플로우:
     * 1. 소셜 플랫폼 → 백엔드 콜백 (인증 코드 전달)
     * 2. 백엔드에서 토큰 발급 및 쿠키 설정
     * 3. 프론트엔드로 안전한 리다이렉트 (토큰 노출 없음)
     */
    @GetMapping("/oauth/kakao")
    public RedirectView handleKakaoCallback(
            @RequestParam(required = false) final String code,
            @RequestParam(required = false) final String state,
            @RequestParam(required = false) final String error,
            final HttpServletResponse response) {

        log.info("Kakao OAuth callback received - error: {}, hasCode: {}, hasState: {}, environment: {}", 
                error, code != null, state != null, getCurrentProfile());

        // 에러 처리
        if (error != null) {
            return redirectToError(error, "kakao");
        }

        // 인증 코드 필수 검증
        if (code == null || code.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.SOCIAL_TOKEN_EXCHANGE_FAILED);
        }

        // 중복 코드 사용 방지
        if (USED_CODES.contains(code)) {
            log.warn("Kakao OAuth code already used: {}...", maskSensitiveData(code));
            return redirectToError("code_already_used", "kakao");
        }

        try {
            // 코드를 사용된 목록에 추가 (중복 방지)
            USED_CODES.add(code);
            
            // 소셜로그인 처리
            final SocialLoginService kakaoService = socialLoginServiceFactory.getService(SocialProvider.KAKAO);
            final SocialLoginResponse loginResponse = kakaoService.processLogin(code);

            // JWT 토큰을 쿠키로 설정
            setSecureCookies(response, loginResponse);

            // 프론트엔드 성공 페이지로 리다이렉트
            final RedirectView redirectView = new RedirectView();
            redirectView.setContextRelative(false);
            final String redirectUrl = buildFrontendUrl("/auth/success");
            redirectView.setUrl(redirectUrl);

            log.info("Kakao login successful for user: {} (env: {})", 
                    maskSensitiveData(loginResponse.memberInfo().email()), getCurrentProfile());
            log.info("Redirecting to: {}", redirectUrl);

            return redirectView;
        } catch (final Exception e) {
            // 실패시 코드를 사용된 목록에서 제거 (재시도 가능하도록)
            USED_CODES.remove(code);
            throw e;
        }
    }

    /**
     * 구글 OAuth 콜백 처리.
     */
    @GetMapping("/oauth/google")
    public RedirectView handleGoogleCallback(
            @RequestParam(required = false) final String code,
            @RequestParam(required = false) final String state,
            @RequestParam(required = false) final String error,
            final HttpServletResponse response) {

        log.info("Google OAuth callback received - error: {}, hasCode: {}, hasState: {}, environment: {}", 
                error, code != null, state != null, getCurrentProfile());

        // 에러 처리
        if (error != null) {
            return redirectToError(error, "google");
        }

        // 인증 코드 필수 검증
        if (code == null || code.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.SOCIAL_TOKEN_EXCHANGE_FAILED);
        }

        // 중복 코드 사용 방지
        if (USED_CODES.contains(code)) {
            log.warn("Google OAuth code already used: {}...", maskSensitiveData(code));
            return redirectToError("code_already_used", "google");
        }

        try {
            // 코드를 사용된 목록에 추가 (중복 방지)
            USED_CODES.add(code);
            
            // 소셜로그인 처리
            final SocialLoginService googleService = socialLoginServiceFactory.getService(SocialProvider.GOOGLE);
            final SocialLoginResponse loginResponse = googleService.processLogin(code);

            // JWT 토큰을 쿠키로 설정
            setSecureCookies(response, loginResponse);

            // 프론트엔드 성공 페이지로 리다이렉트
            final RedirectView redirectView = new RedirectView();
            redirectView.setContextRelative(false);
            final String redirectUrl = buildFrontendUrl("/auth/success");
            redirectView.setUrl(redirectUrl);

            log.info("Google login successful for user: {} (env: {})", 
                    maskSensitiveData(loginResponse.memberInfo().email()), getCurrentProfile());
            log.info("Redirecting to: {}", redirectUrl);

            return redirectView;
        } catch (final Exception e) {
            // 실패시 코드를 사용된 목록에서 제거 (재시도 가능하도록)
            USED_CODES.remove(code);
            throw e;
        }
    }

    /**
     * 소셜 로그인 처리 API (프론트엔드에서 직접 호출용).
     */
    @PostMapping("/social/login/{provider}")
    public ResponseEntity<ApiResponseFormat<SocialLoginResponse>> processSocialLogin(
            @PathVariable final String provider,
            @RequestParam final String code,
            final HttpServletResponse response) {
        
        log.info("Processing social login for provider: {} with code", provider);
        
        // 소셜 제공자 확인
        final SocialProvider socialProvider;
        try {
            socialProvider = SocialProvider.valueOf(provider.toUpperCase());
        } catch (final IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_SOCIAL_PROVIDER, provider);
        }
        
        // 소셜 로그인 처리
        final SocialLoginService loginService = socialLoginServiceFactory.getService(socialProvider);
        final SocialLoginResponse loginResponse = loginService.processLogin(code);
        
        // JWT 토큰을 쿠키로 설정
        setSecureCookies(response, loginResponse);
        
        // 보안을 위해 토큰 정보는 마스킹해서 응답
        final SocialLoginResponse secureResponse = createSecureResponse(loginResponse);
        
        log.info("{} login successful for user: {} (env: {})", 
                provider, maskSensitiveData(loginResponse.memberInfo().email()), getCurrentProfile());
        
        return ResponseEntity.ok(
                ApiResponseFormat.success("소셜 로그인이 완료되었습니다.", secureResponse)
        );
    }

    /**
     * 소셜 로그인 URL 생성 API
     */
    @GetMapping("/social/auth-url/{provider}")
    public ResponseEntity<ApiResponseFormat<Object>> getSocialAuthUrl(@PathVariable final String provider) {
        log.info("Generating auth URL for provider: {}", provider);
        
        // 소셜 제공자 확인
        final SocialProvider socialProvider;
        try {
            socialProvider = SocialProvider.valueOf(provider.toUpperCase());
        } catch (final IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_SOCIAL_PROVIDER, provider);
        }
        
        // 인증 URL 생성
        final String authUrl = generateAuthUrl(socialProvider);
        final String state = generateSecureState();
        
        final Object response = java.util.Map.of(
            "provider", provider,
            "authUrl", authUrl,
            "state", state,
            "redirectUri", environment.getProperty("oauth2." + provider.toLowerCase() + ".redirect-uri", "NOT_SET")
        );
        
        log.info("Generated auth URL for {}: {}", provider, authUrl);
        
        return ResponseEntity.ok(
                ApiResponseFormat.success("소셜 로그인 URL이 생성되었습니다.", response)
        );
    }

    @GetMapping("/debug/clear-used-codes")
    public ResponseEntity<ApiResponseFormat<Object>> clearUsedCodes() {
        if (isProductionEnvironment()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        
        final int clearedCount = USED_CODES.size();
        USED_CODES.clear();
        
        final Object response = java.util.Map.of(
            "clearedCount", clearedCount,
            "message", "사용된 OAuth 코드 캐시가 정리되었습니다."
        );
        
        log.info("Cleared {} used OAuth codes from cache", clearedCount);
        
        return ResponseEntity.ok(
                ApiResponseFormat.success("OAuth 코드 캐시 정리 완료", response)
        );
    }
    @GetMapping("/debug/social-config")
    public ResponseEntity<ApiResponseFormat<Object>> debugSocialConfig() {
        if (isProductionEnvironment()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        
        final Object debugInfo = java.util.Map.of(
            "environment", getCurrentProfile(),
            "kakao", java.util.Map.of(
                "clientId", environment.getProperty("oauth2.kakao.client-id", "NOT_SET"),
                "redirectUri", environment.getProperty("oauth2.kakao.redirect-uri", "NOT_SET"),
                "authUri", environment.getProperty("oauth2.kakao.authorization-uri", "NOT_SET")
            ),
            "google", java.util.Map.of(
                "clientId", environment.getProperty("oauth2.google.client-id", "NOT_SET"),
                "redirectUri", environment.getProperty("oauth2.google.redirect-uri", "NOT_SET"),
                "authUri", environment.getProperty("oauth2.google.authorization-uri", "NOT_SET")
            ),
            "cors", java.util.Map.of(
                "allowedOrigins", environment.getProperty("app.cors.allowed-origins[0]", "NOT_SET"),
                "frontendUrl", getFrontendBaseUrl()
            )
        );
        
        return ResponseEntity.ok(
                ApiResponseFormat.success("소셜 로그인 설정 디버그 정보", debugInfo)
        );
    }

    /**
     * 로그아웃 처리 API.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponseFormat<Void>> logout(final HttpServletResponse response) {
        jwtCookieHelper.clearTokenCookies(response);
        
        log.info("User logged out successfully, cookies cleared (env: {})", getCurrentProfile());
        
        return ResponseEntity.ok(
                ApiResponseFormat.success("로그아웃되었습니다.")
        );
    }

    /**
     * 소셜 제공자 목록 조회 API.
     */
    @GetMapping("/social/providers")
    public ResponseEntity<ApiResponseFormat<GetSocialProvidersResponse>> getSocialProviders() {
        final List<SocialProviderInfo> providers = Arrays.stream(SocialProvider.values())
                .map(provider -> {
                    final String authUrl = generateAuthUrl(provider);
                    final String state = generateSecureState();
                    
                    return SocialProviderInfo.of(
                            provider.name().toLowerCase(),
                            provider.name(),
                            authUrl,
                            state
                    );
                })
                .toList();

        final GetSocialProvidersResponse response = GetSocialProvidersResponse.of(providers);

        log.debug("Social providers response generated for {} environment", getCurrentProfile());

        return ResponseEntity.ok(
                ApiResponseFormat.success("지원 소셜 로그인 목록을 조회했습니다.", response)
        );
    }

    // Private Helper Methods

    /**
     * JWT 토큰을 안전한 쿠키로 설정.
     */
    private void setSecureCookies(final HttpServletResponse response, 
                                  final SocialLoginResponse loginResponse) {
        jwtCookieHelper.setAccessTokenCookie(response, loginResponse.accessToken());
        
        if (loginResponse.refreshToken() != null) {
            jwtCookieHelper.setRefreshTokenCookie(response, loginResponse.refreshToken());
        }

        log.debug("Secure cookies set successfully for {} environment", getCurrentProfile());
    }

    /**
     * 보안을 위해 토큰 정보를 마스킹한 응답 생성
     */
    private SocialLoginResponse createSecureResponse(final SocialLoginResponse original) {
        return SocialLoginResponse.of(
                maskSensitiveData(original.accessToken()),
                original.refreshToken() != null ? maskSensitiveData(original.refreshToken()) : null,
                original.expiresIn(),
                original.memberInfo()
        );
    }

    /**
     * 에러 페이지로 리다이렉트하는 RedirectView 생성.
     */
    private RedirectView redirectToError(final String errorMessage, final String provider) {
        final RedirectView redirectView = new RedirectView();
        redirectView.setContextRelative(false);

        
        final String errorUrl = buildFrontendUrl("/auth/error") 
                + "?message=" + URLEncoder.encode(errorMessage, StandardCharsets.UTF_8)
                + "&provider=" + URLEncoder.encode(provider, StandardCharsets.UTF_8);
        
        redirectView.setUrl(errorUrl);
        
        log.warn("{} authentication failed: {} (env: {})", provider, errorMessage, getCurrentProfile());
        
        return redirectView;
    }

    /**
     * 환경별 프론트엔드 URL 빌드.
     */
    private String buildFrontendUrl(final String path) {
        final String baseUrl = getFrontendBaseUrl();
        final String normalizedPath = path.startsWith("/") ? path : "/" + path;
        
        final String fullUrl = baseUrl + normalizedPath;
        log.debug("Built frontend URL: {} (env: {})", fullUrl, getCurrentProfile());
        
        return fullUrl;
    }

    /**
     * 환경별 프론트엔드 기본 URL 결정.
     */
    private String getFrontendBaseUrl() {
        try {
            // 설정에서 프론트엔드 URL 가져오기
            final String frontendUrl = environment.getProperty("app.frontend.base-url");
            if (frontendUrl != null && !frontendUrl.trim().isEmpty()) {
                return frontendUrl.trim();
            }
            
            // 설정이 없을 경우 fallback
            return isProductionEnvironment() ? "https://www.ururu.shop" : "http://localhost:3000";
            
        } catch (final Exception e) {
            log.warn("Failed to get frontend URL from yml config, using fallback (env: {}): {}", 
                    getCurrentProfile(), e.getMessage());
            return isProductionEnvironment() ? "https://www.ururu.shop" : "http://localhost:3000";
        }
    }

    /**
     * 환경별 소셜 인증 URL 생성.
     */
    private String generateAuthUrl(final SocialProvider provider) {
        final SocialLoginService loginService = socialLoginServiceFactory.getService(provider);
        final String state = generateSecureState();
        return loginService.getAuthorizationUrl(state);
    }

    /**
     * CSRF 방지를 위한 안전한 state 생성
     */
    private String generateSecureState() {
        final byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return BASE64_ENCODER.encodeToString(randomBytes);
    }

    /**
     * 민감한 데이터 마스킹 (로그용)
     */
    private String maskSensitiveData(final String data) {
        if (data == null || data.length() <= SENSITIVE_DATA_PREVIEW_LENGTH) {
            return MASKED_DATA_PLACEHOLDER;
        }
        return data.substring(0, SENSITIVE_DATA_PREVIEW_LENGTH) + "...";
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

    // DTOs

    /**
     * 소셜 제공자 정보 DTO
     */
    public record SocialProviderInfo(
            String id,
            String name,
            String authUrl,
            String state
    ) {
        public static SocialProviderInfo of(final String id, final String name, 
                                          final String authUrl, final String state) {
            return new SocialProviderInfo(id, name, authUrl, state);
        }
    }

    /**
     * 소셜 제공자 목록 응답 DTO
     */
    public record GetSocialProvidersResponse(
            List<SocialProviderInfo> providers
    ) {
        public static GetSocialProvidersResponse of(final List<SocialProviderInfo> providers) {
            return new GetSocialProvidersResponse(providers);
        }
    }
}
