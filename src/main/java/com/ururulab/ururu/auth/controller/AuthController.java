package com.ururulab.ururu.auth.controller;

import com.ururulab.ururu.auth.annotation.RateLimit;
import com.ururulab.ururu.auth.constants.AuthConstants;
import com.ururulab.ururu.auth.constants.UserType;
import com.ururulab.ururu.auth.dto.response.SocialLoginResponse;
import com.ururulab.ururu.auth.jwt.JwtCookieHelper;
import com.ururulab.ururu.auth.jwt.JwtTokenProvider;
import com.ururulab.ururu.auth.jwt.token.AccessTokenGenerator;
import com.ururulab.ururu.auth.service.*;
import com.ururulab.ururu.auth.util.AuthCookieHelper;
import com.ururulab.ururu.auth.util.AuthResponseHelper;
import com.ururulab.ururu.auth.util.TokenExtractor;
import com.ururulab.ururu.auth.util.EnvironmentHelper;
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
import org.springframework.web.servlet.view.RedirectView;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 인증 관련 API 컨트롤러.
 * 
 * <p>OAuth 콜백 처리, 토큰 갱신, 로그아웃 등의 인증 관련 기능을 제공합니다.
 * 소셜 로그인과 JWT 토큰 관리를 담당하며, 보안을 위한 다양한 검증을 수행합니다.</p>
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
    private final EnvironmentHelper environmentHelper;
    private final CsrfTokenService csrfTokenService;

    // ==================== OAuth 콜백 처리 ====================

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

    // ==================== 소셜 로그인 API ====================

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
        
        final SocialProvider socialProvider = validateSocialProvider(provider);
        final SocialLoginService loginService = socialLoginServiceFactory.getService(socialProvider);
        final SocialLoginResponse loginResponse = loginService.processLogin(code);
        
        AuthCookieHelper.setSecureCookies(response, loginResponse, jwtCookieHelper);
        
        // CSRF 토큰 생성 및 응답 헤더에 추가
        final String csrfToken = csrfTokenService.generateCsrfToken(
                loginResponse.memberInfo().memberId(), 
                loginResponse.memberInfo().userType()
        );
        response.setHeader("X-CSRF-TOKEN", csrfToken);
        
        final SocialLoginResponse secureResponse = AuthResponseHelper.createSecureResponse(loginResponse, securityLoggingService);
        
        log.info("{} login successful for user: {} (env: {})", 
                provider, MaskingUtils.maskEmail(loginResponse.memberInfo().email()), environmentHelper.getCurrentProfile());
        
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
        
        final SocialProvider socialProvider = validateSocialProvider(provider);
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

    // ==================== 토큰 관리 API ====================

    /**
     * 토큰 갱신 API.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponseFormat<SocialLoginResponse>> refreshToken(
            @CookieValue(name = "refresh_token", required = false) final String refreshToken,
            final HttpServletResponse response) {
        
        if (!TokenExtractor.isValidRefreshToken(refreshToken)) {
            throw new BusinessException(ErrorCode.MISSING_REFRESH_TOKEN);
        }
        
        final SocialLoginResponse refreshResponse = jwtRefreshService.refreshAccessToken(refreshToken);
        AuthCookieHelper.setSecureCookies(response, refreshResponse, jwtCookieHelper);
        
        // 새로운 CSRF 토큰 생성 및 응답 헤더에 추가
        final String newCsrfToken = jwtRefreshService.generateNewCsrfToken(
                refreshResponse.memberInfo().memberId(), 
                refreshResponse.memberInfo().userType()
        );
        response.setHeader("X-CSRF-TOKEN", newCsrfToken);
        
        final SocialLoginResponse secureResponse = AuthResponseHelper.createSecureResponse(refreshResponse, securityLoggingService);
        
        log.info("Token refresh successful for user: {} (env: {})", 
                MaskingUtils.maskEmail(refreshResponse.memberInfo().email()), environmentHelper.getCurrentProfile());
        
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
        
        final String tokenToLogout = TokenExtractor.extractTokenForLogout(authorization, accessToken);
        
        if (tokenToLogout != null) {
            try {
                jwtRefreshService.logoutWithToken(tokenToLogout);
            } catch (final Exception e) {
                log.warn("Failed to logout from Redis: {}", e.getMessage());
            }
        } else if (TokenExtractor.isValidAccessToken(accessToken)) {
            try {
                jwtRefreshService.logoutWithToken(accessToken);
            } catch (final Exception e) {
                log.warn("Failed to logout from Redis using cookie token: {}", e.getMessage());
            }
        }
        
        jwtCookieHelper.clearTokenCookies(response);
        
        log.info("User logged out successfully, cookies cleared (env: {})", environmentHelper.getCurrentProfile());
        
        return ResponseEntity.ok(
                ApiResponseFormat.success("로그아웃되었습니다.")
        );
    }

    // ==================== 정보 조회 API ====================

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

        log.debug("Social providers response generated for {} environment", environmentHelper.getCurrentProfile());

        return ResponseEntity.ok(
                ApiResponseFormat.success("지원 소셜 로그인 목록을 조회했습니다.", response)
        );
    }

    /**
     * 현재 인증 상태 조회 API.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponseFormat<SocialLoginResponse>> getCurrentAuthStatus(
            @CookieValue(name = "access_token", required = false) final String accessToken,
            final HttpServletResponse response) {
        
        if (TokenExtractor.isValidAccessToken(accessToken)) {
            try {
                final TokenValidator.TokenValidationResult validationResult = tokenValidator.validateAccessToken(accessToken);
                final UserInfoService.UserInfo userInfo = userInfoService.getUserInfo(validationResult.userId(), UserType.fromString(validationResult.userType()));
                
                final SocialLoginResponse authResponse = SocialLoginResponse.of(
                        accessToken,
                        null,
                        accessTokenGenerator.getExpirySeconds(),
                        SocialLoginResponse.MemberInfo.of(validationResult.userId(), userInfo.email(), null, null, validationResult.userType())
                );
                
                final SocialLoginResponse secureResponse = AuthResponseHelper.createSecureResponse(authResponse, securityLoggingService);
                
                log.debug("Current auth status retrieved for user: {} (type: {}) - using existing access token", 
                        validationResult.userId(), validationResult.userType());
                
                return ResponseEntity.ok(
                        ApiResponseFormat.success("현재 인증 상태를 조회했습니다.", secureResponse)
                );
            } catch (final Exception e) {
                log.debug("Access token validation failed: {}", e.getMessage());
            }
        }
        
        return ResponseEntity.status(401).body(
                ApiResponseFormat.success("인증되지 않은 사용자입니다.")
        );
    }

    // ==================== Private Helper Methods ====================

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
                providerName, error, code != null, state != null, environmentHelper.getCurrentProfile());

        if (error != null) {
            return redirectToError(error, providerName);
        }

        validateOAuthParameters(code, state, providerName);

        final String redisKey = AuthConstants.OAUTH_CODE_KEY_PREFIX + code;
        final Boolean isCodeUsed = redisTemplate.opsForValue().setIfAbsent(redisKey, "1", Duration.ofSeconds(AuthConstants.OAUTH_CODE_TTL_SECONDS));

        if (isCodeUsed == null || !isCodeUsed) {
            log.warn("{} OAuth code already used or expired: {}...", providerName, securityLoggingService.maskSensitiveData(code));
            return redirectToError("code_already_used", providerName);
        }

        try {
            validateOAuthState(state, providerName);
            final SocialLoginResponse loginResponse = processOAuthLogin(provider, code);
            AuthCookieHelper.setSecureCookies(response, loginResponse, jwtCookieHelper);
            
            // CSRF 토큰 생성 및 응답 헤더에 추가
            final String csrfToken = csrfTokenService.generateCsrfToken(
                    loginResponse.memberInfo().memberId(), 
                    loginResponse.memberInfo().userType()
            );
            response.setHeader("X-CSRF-TOKEN", csrfToken);
            
            cleanupOAuthData(code, state);
            
            final RedirectView redirectView = createSuccessRedirectView();
            
            log.info("{} login successful for user: {} (env: {})",
                    providerName, securityLoggingService.maskEmail(loginResponse.memberInfo().email()), environmentHelper.getCurrentProfile());
            
            return redirectView;
        } catch (final BusinessException e) {
            cleanupOAuthData(code, state);
            return handleBusinessExceptionRedirect(e, providerName);
        } catch (final Exception e) {
            cleanupOAuthData(code, state);
            throw e;
        }
    }

    /**
     * OAuth 파라미터 검증
     */
    private void validateOAuthParameters(final String code, final String state, final String providerName) {
        if (!TokenExtractor.isValidOAuthParameter(code)) {
            throw new BusinessException(ErrorCode.SOCIAL_TOKEN_EXCHANGE_FAILED);
        }

        if (!TokenExtractor.isValidOAuthParameter(state)) {
            log.warn("{} OAuth callback received without state parameter", providerName);
            throw new BusinessException(ErrorCode.SOCIAL_TOKEN_EXCHANGE_FAILED);
        }
    }

    /**
     * OAuth state 검증
     */
    private void validateOAuthState(final String state, final String providerName) {
        final String redisStateKey = AuthConstants.OAUTH_CODE_KEY_PREFIX + "state:" + state;
        final Boolean isStateUsed = redisTemplate.opsForValue().setIfAbsent(redisStateKey, "1", Duration.ofSeconds(AuthConstants.OAUTH_CODE_TTL_SECONDS));

        if (isStateUsed == null || !isStateUsed) {
            log.warn("{} OAuth state already used or expired: {}...", providerName, securityLoggingService.maskSensitiveData(state));
            throw new BusinessException(ErrorCode.SOCIAL_TOKEN_EXCHANGE_FAILED);
        }
    }

    /**
     * OAuth 로그인 처리
     */
    private SocialLoginResponse processOAuthLogin(final SocialProvider provider, final String code) {
        final SocialLoginService loginService = socialLoginServiceFactory.getService(provider);
        return loginService.processLogin(code);
    }

    /**
     * OAuth 데이터 정리
     */
    private void cleanupOAuthData(final String code, final String state) {
        redisTemplate.delete(AuthConstants.OAUTH_CODE_KEY_PREFIX + code);
        redisTemplate.delete(AuthConstants.OAUTH_CODE_KEY_PREFIX + "state:" + state);
    }

    /**
     * 성공 리다이렉트 뷰 생성
     */
    private RedirectView createSuccessRedirectView() {
        final RedirectView redirectView = new RedirectView();
        redirectView.setContextRelative(false);
        final String redirectUrl = buildFrontendUrl("/auth/success");
        redirectView.setUrl(redirectUrl);
        log.info("Redirecting to: {}", redirectUrl);
        return redirectView;
    }

    /**
     * 비즈니스 예외 리다이렉트 처리
     */
    private RedirectView handleBusinessExceptionRedirect(final BusinessException e, final String provider) {
        final ErrorCode errorCode = e.getErrorCode();
        String errorReason = "login_failed";

        if (errorCode == ErrorCode.MEMBER_DELETED || errorCode == ErrorCode.MEMBER_LOGIN_DENIED) {
            errorReason = "withdrawn_member";
        } else if (errorCode == ErrorCode.SOCIAL_TOKEN_EXCHANGE_FAILED) {
            errorReason = "token_exchange_failed";
        } else if (errorCode == ErrorCode.SOCIAL_MEMBER_INFO_FAILED) {
            errorReason = "member_info_failed";
        }

        log.warn("{} authentication failed with business exception: {} ({})",
                provider, errorCode.getCode(), e.getMessage());
        return redirectToError(errorReason, provider);
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
     * 에러 페이지로 리다이렉트하는 RedirectView 생성.
     */
    private RedirectView redirectToError(final String errorMessage, final String provider) {
        final RedirectView redirectView = new RedirectView();
        redirectView.setContextRelative(false);

        final String errorUrl = buildFrontendUrl("/auth/callback")
                + "?result=error"
                + "&reason=" + URLEncoder.encode(errorMessage, StandardCharsets.UTF_8)
                + "&provider=" + URLEncoder.encode(provider, StandardCharsets.UTF_8);

        redirectView.setUrl(errorUrl);
        
        log.warn("{} authentication failed: {} (env: {})", provider, errorMessage, environmentHelper.getCurrentProfile());
        
        return redirectView;
    }

    /**
     * 환경별 프론트엔드 URL 빌드.
     */
    private String buildFrontendUrl(final String path) {
        final String baseUrl = getFrontendBaseUrl();
        final String normalizedPath = path.startsWith("/") ? path : "/" + path;
        
        final String fullUrl = baseUrl + normalizedPath;
        log.debug("Built frontend URL: {}", fullUrl);
        
        return fullUrl;
    }

    /**
     * 환경별 프론트엔드 기본 URL 결정.
     */
    private String getFrontendBaseUrl() {
        return environmentHelper.getFrontendBaseUrl();
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



    // ==================== DTOs ====================

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
