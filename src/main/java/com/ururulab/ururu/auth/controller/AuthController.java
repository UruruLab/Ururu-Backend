package com.ururulab.ururu.auth.controller;

import com.ururulab.ururu.auth.dto.response.SocialLoginResponse;
import com.ururulab.ururu.auth.jwt.JwtCookieHelper;
import com.ururulab.ururu.auth.jwt.JwtTokenProvider;
import com.ururulab.ururu.auth.jwt.token.AccessTokenGenerator;
import com.ururulab.ururu.auth.service.SocialLoginServiceFactory;
import com.ururulab.ururu.auth.service.SocialLoginService;
import com.ururulab.ururu.auth.service.JwtRefreshService;
import com.ururulab.ururu.auth.service.UserInfoService;
import com.ururulab.ururu.auth.service.TokenValidator;
import com.ururulab.ururu.auth.util.TokenExtractor;
import com.ururulab.ururu.auth.annotation.RateLimit;
import com.ururulab.ururu.global.domain.dto.ApiResponseFormat;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.global.util.MaskingUtils;
import com.ururulab.ururu.member.domain.entity.enumerated.SocialProvider;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.servlet.view.RedirectView;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;
import com.ururulab.ururu.auth.service.SecurityLoggingService;
import com.ururulab.ururu.auth.util.AuthResponseHelper;
import com.ururulab.ururu.auth.util.AuthCookieHelper;
import com.ururulab.ururu.auth.constants.AuthConstants;

/**
 * 인증 관련 API 컨트롤러.
 * 
 * OAuth 콜백 처리, 토큰 갱신, 로그아웃 등의 인증 관련 기능을 제공합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder BASE64_ENCODER = Base64.getUrlEncoder();

    private final SocialLoginServiceFactory socialLoginServiceFactory;
    private final JwtCookieHelper jwtCookieHelper;
    private final JwtRefreshService jwtRefreshService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AccessTokenGenerator accessTokenGenerator;
    private final UserInfoService userInfoService;
    private final TokenValidator tokenValidator;
    private final Environment environment;
    private final StringRedisTemplate redisTemplate;
    private final SecurityLoggingService securityLoggingService;

    /**
     * 카카오 OAuth 콜백 처리.
     */
    @GetMapping("/oauth/kakao")
    public RedirectView handleKakaoCallback(
            @RequestParam(required = false) final String code,
            @RequestParam(required = false) final String state,
            @RequestParam(required = false) final String error,
            final HttpServletResponse response) {

        return handleOAuthCallback(code, state, error, SocialProvider.KAKAO, response);
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

        return handleOAuthCallback(code, state, error, SocialProvider.GOOGLE, response);
    }

    /**
     * 소셜 로그인 처리 API (프론트엔드에서 직접 호출용).
     */
    @RateLimit(value = 5, timeUnit = TimeUnit.MINUTES)
    @PostMapping("/social/login/{provider}")
    public ResponseEntity<ApiResponseFormat<SocialLoginResponse>> processSocialLogin(
            @PathVariable final String provider,
            @RequestParam final String code,
            final HttpServletResponse response) {
        
        log.info("Processing social login for provider: {} with code", provider);
        
        // 소셜 제공자 확인
        final SocialProvider socialProvider = validateSocialProvider(provider);
        
        // 소셜 로그인 처리
        final SocialLoginService loginService = socialLoginServiceFactory.getService(socialProvider);
        final SocialLoginResponse loginResponse = loginService.processLogin(code);
        
        // JWT 토큰을 쿠키로 설정
        setSecureCookies(response, loginResponse);
        
        // 보안을 위해 토큰 정보는 마스킹해서 응답
        final SocialLoginResponse secureResponse = createSecureResponse(loginResponse);
        
        log.info("{} login successful for user: {} (env: {})", 
                provider, MaskingUtils.maskEmail(loginResponse.memberInfo().email()), getCurrentProfile());
        
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
        final SocialProvider socialProvider = validateSocialProvider(provider);
        
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

    /**
     * 토큰 갱신 API.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponseFormat<SocialLoginResponse>> refreshToken(
            @CookieValue(name = "refresh_token", required = false) final String refreshToken,
            final HttpServletResponse response) {
        
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.MISSING_REFRESH_TOKEN);
        }
        
        final SocialLoginResponse refreshResponse = jwtRefreshService.refreshAccessToken(refreshToken);
        
        // 새로운 토큰을 쿠키로 설정
        jwtCookieHelper.setAccessTokenCookie(response, refreshResponse.accessToken());
        if (refreshResponse.refreshToken() != null) {
            jwtCookieHelper.setRefreshTokenCookie(response, refreshResponse.refreshToken());
        }
        
        // 보안을 위해 토큰 정보는 마스킹해서 응답
        final SocialLoginResponse secureResponse = createSecureResponse(refreshResponse);
        
        log.info("Token refresh successful for user: {} (env: {})", 
                MaskingUtils.maskEmail(refreshResponse.memberInfo().email()), getCurrentProfile());
        
        return ResponseEntity.ok(
                ApiResponseFormat.success("토큰이 갱신되었습니다.", secureResponse)
        );
    }

    /**
     * 로그아웃 처리 API.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponseFormat<Void>> logout(
            @RequestHeader(name = "Authorization", required = false) final String authorization,
            @CookieValue(name = "access_token", required = false) final String accessToken,
            @CookieValue(name = "refresh_token", required = false) final String refreshToken,
            final HttpServletResponse response) {
        
        // TokenExtractor를 사용하여 토큰 추출
        final String tokenToLogout = TokenExtractor.extractTokenForLogout(authorization, accessToken);
        
        if (tokenToLogout != null) {
            try {
                jwtRefreshService.logout(tokenToLogout);
            } catch (final Exception e) {
                log.warn("Failed to logout from Redis: {}", e.getMessage());
                // Redis 삭제 실패는 로그아웃을 중단시키지 않음
            }
        } else if (TokenExtractor.isValidAccessToken(accessToken)) {
            // Authorization 헤더가 없어도 쿠키에서 토큰 추출하여 로그아웃 처리
            try {
                jwtRefreshService.logoutWithToken(accessToken);
            } catch (final Exception e) {
                log.warn("Failed to logout from Redis using cookie token: {}", e.getMessage());
                // Redis 삭제 실패는 로그아웃을 중단시키지 않음
            }
        }
        
        // 쿠키 삭제
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

    /**
     * 현재 인증 상태 조회 API.
     * 액세스 토큰이 유효하면 기존 토큰을 그대로 사용하고,
     * 액세스 토큰이 만료된 경우에만 리프레시 토큰으로 갱신합니다.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponseFormat<SocialLoginResponse>> getCurrentAuthStatus(
            @CookieValue(name = "access_token", required = false) final String accessToken,
            @CookieValue(name = "refresh_token", required = false) final String refreshToken,
            final HttpServletResponse response) {
        
        // 액세스 토큰이 있으면 검증
        if (accessToken != null && !accessToken.isBlank()) {
            try {
                // TokenValidator를 사용하여 토큰 검증 및 사용자 정보 추출
                final TokenValidator.TokenValidationResult validationResult = tokenValidator.validateAccessToken(accessToken);
                
                // 사용자 정보 조회
                final UserInfoService.UserInfo userInfo = userInfoService.getUserInfo(validationResult.userId(), validationResult.userType());
                
                // 기존 토큰을 그대로 사용 (새로운 리프레시 토큰 발급하지 않음)
                final SocialLoginResponse authResponse = SocialLoginResponse.of(
                        accessToken,
                        refreshToken, // 기존 리프레시 토큰 그대로 사용
                        accessTokenGenerator.getExpirySeconds(),
                        SocialLoginResponse.MemberInfo.of(validationResult.userId(), userInfo.email(), null, null, validationResult.userType())
                );
                
                // 보안을 위해 토큰 정보는 마스킹해서 응답
                final SocialLoginResponse secureResponse = createSecureResponse(authResponse);
                
                log.debug("Current auth status retrieved for user: {} (type: {}) - using existing tokens", validationResult.userId(), validationResult.userType());
                
                return ResponseEntity.ok(
                        ApiResponseFormat.success("현재 인증 상태를 조회했습니다.", secureResponse)
                );
            } catch (final Exception e) {
                log.debug("Access token validation failed: {}", e.getMessage());
                // 액세스 토큰이 유효하지 않으면 리프레시 토큰으로 갱신 시도
            }
        }
        
        // 리프레시 토큰이 있으면 갱신 시도 (액세스 토큰이 만료된 경우에만)
        if (refreshToken != null && !refreshToken.isBlank()) {
            try {
                final SocialLoginResponse refreshResponse = jwtRefreshService.refreshAccessToken(refreshToken);
                
                // 새로운 토큰을 쿠키로 설정
                jwtCookieHelper.setAccessTokenCookie(response, refreshResponse.accessToken());
                if (refreshResponse.refreshToken() != null) {
                    jwtCookieHelper.setRefreshTokenCookie(response, refreshResponse.refreshToken());
                }
                
                // 보안을 위해 토큰 정보는 마스킹해서 응답
                final SocialLoginResponse secureResponse = createSecureResponse(refreshResponse);
                
                log.info("Token refresh successful for user: {} (env: {})", 
                        MaskingUtils.maskEmail(refreshResponse.memberInfo().email()), getCurrentProfile());
                
                return ResponseEntity.ok(
                        ApiResponseFormat.success("토큰이 갱신되었습니다.", secureResponse)
                );
            } catch (final Exception e) {
                log.debug("Refresh token validation failed: {}", e.getMessage());
            }
        }
        
        // 인증되지 않은 상태
        return ResponseEntity.status(401).body(
                ApiResponseFormat.success("인증되지 않은 사용자입니다.")
        );
    }

    // Private Helper Methods

    /**
     * OAuth 콜백 공통 처리 메서드
     */
    private RedirectView handleOAuthCallback(
            final String code,
            final String state,
            final String error, 
            final SocialProvider provider,
            final HttpServletResponse response) {
        
        final String providerName = provider.name().toLowerCase();
        
        log.info("{} OAuth callback received - error: {}, hasCode: {}, hasState: {}, environment: {}", 
                providerName, error, code != null, state != null, getCurrentProfile());

        // 에러 처리
        if (error != null) {
            return redirectToError(error, providerName);
        }

        // 인증 코드 필수 검증
        if (code == null || code.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.SOCIAL_TOKEN_EXCHANGE_FAILED);
        }

        // state 검증 (CSRF 공격 방지)
        if (state == null || state.trim().isEmpty()) {
            log.warn("{} OAuth callback received without state parameter", providerName);
            return redirectToError("missing_state", providerName);
        }

        // 중복 코드 사용 방지
        final String redisKey = AuthConstants.OAUTH_CODE_KEY_PREFIX + code;
        final Boolean isCodeUsed = redisTemplate.opsForValue().setIfAbsent(redisKey, "1", Duration.ofSeconds(AuthConstants.OAUTH_CODE_TTL_SECONDS));

        if (isCodeUsed == null || !isCodeUsed) {
            log.warn("{} OAuth code already used or expired: {}...", providerName, securityLoggingService.maskSensitiveData(code));
            return redirectToError("code_already_used", providerName);
        }

        try {
            // state를 Redis에 저장 (중복 방지)
            final String redisStateKey = AuthConstants.OAUTH_CODE_KEY_PREFIX + "state:" + state;
            final Boolean isStateUsed = redisTemplate.opsForValue().setIfAbsent(redisStateKey, "1", Duration.ofSeconds(AuthConstants.OAUTH_CODE_TTL_SECONDS));
            
            if (isStateUsed == null || !isStateUsed) {
                log.warn("{} OAuth state already used or expired: {}...", providerName, securityLoggingService.maskSensitiveData(state));
                return redirectToError("state_already_used", providerName);
            }
            
            // 소셜로그인 처리
            final SocialLoginService loginService = socialLoginServiceFactory.getService(provider);
            final SocialLoginResponse loginResponse = loginService.processLogin(code);

            // JWT 토큰을 쿠키로 설정
            setSecureCookies(response, loginResponse);

            // 프론트엔드 성공 페이지로 리다이렉트
            final RedirectView redirectView = new RedirectView();
            redirectView.setContextRelative(false);
            final String redirectUrl = buildFrontendUrl("/auth/success");
            redirectView.setUrl(redirectUrl);

            log.info("{} login successful for user: {} (env: {})", 
                    providerName, securityLoggingService.maskEmail(loginResponse.memberInfo().email()), getCurrentProfile());
            log.info("Redirecting to: {}", redirectUrl);

            return redirectView;
        } catch (final Exception e) {
            // 실패시 코드와 state를 사용된 목록에서 제거 (재시도 가능하도록)
            redisTemplate.delete(AuthConstants.OAUTH_CODE_KEY_PREFIX + code);
            redisTemplate.delete(AuthConstants.OAUTH_CODE_KEY_PREFIX + "state:" + state);
            throw e;
        }
    }

    /**
     * 소셜 제공자 유효성 검증
     */
    private SocialProvider validateSocialProvider(final String provider) {
        try {
            return SocialProvider.valueOf(provider.toUpperCase());
        } catch (final IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_SOCIAL_PROVIDER, provider);
        }
    }

    /**
     * JWT 토큰을 안전한 쿠키로 설정.
     */
    private void setSecureCookies(final HttpServletResponse response, 
                                  final SocialLoginResponse loginResponse) {
        AuthCookieHelper.setSecureCookies(response, loginResponse, jwtCookieHelper);
        log.debug("Secure cookies set successfully for {} environment", getCurrentProfile());
    }

    /**
     * 보안을 위해 토큰 정보를 마스킹한 응답 생성
     */
    private SocialLoginResponse createSecureResponse(final SocialLoginResponse original) {
        return AuthResponseHelper.createSecureResponse(original, securityLoggingService);
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
            return isProductionEnvironment() ? AuthConstants.FRONTEND_BASE_URL_PROD : AuthConstants.FRONTEND_BASE_URL_DEV;
            
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
