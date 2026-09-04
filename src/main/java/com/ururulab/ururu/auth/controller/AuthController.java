package com.ururulab.ururu.auth.controller;

import com.ururulab.ururu.auth.annotation.RateLimit;
import com.ururulab.ururu.auth.dto.response.SocialLoginResponse;
import com.ururulab.ururu.auth.jwt.JwtCookieHelper;
import com.ururulab.ururu.auth.jwt.token.AccessTokenGenerator;
import com.ururulab.ururu.auth.service.JwtRefreshService;
import com.ururulab.ururu.auth.service.TokenValidator;
import com.ururulab.ururu.auth.service.UserInfoService;
import com.ururulab.ururu.auth.util.TokenExtractor;
import com.ururulab.ururu.global.domain.dto.ApiResponseFormat;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.global.util.MaskingUtils;
import com.ururulab.ururu.member.domain.entity.enumerated.SocialProvider;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String OAUTH2_AUTHORIZATION_BASE_URI = "/oauth2/authorization/";

    private final JwtCookieHelper jwtCookieHelper;
    private final JwtRefreshService jwtRefreshService;
    private final AccessTokenGenerator accessTokenGenerator;
    private final UserInfoService userInfoService;
    private final TokenValidator tokenValidator;

    @GetMapping("/csrf")
    public ResponseEntity<ApiResponseFormat<Void>> csrf(final CsrfToken csrfToken) {
        csrfToken.getToken();
        return ResponseEntity.ok(ApiResponseFormat.success("CSRF 쿠키가 발급되었습니다."));
    }

    @GetMapping("/social/auth-url/{provider}")
    @RateLimit(value = 30, timeUnit = TimeUnit.MINUTES)
    public ResponseEntity<ApiResponseFormat<Object>> getSocialAuthUrl(@PathVariable final String provider) {
        final String registrationId = registrationId(provider);
        return ResponseEntity.ok(ApiResponseFormat.success(
                "소셜 로그인 URL이 생성되었습니다.",
                Map.of("provider", registrationId, "authUrl", OAUTH2_AUTHORIZATION_BASE_URI + registrationId)
        ));
    }

    @GetMapping("/social/providers")
    @RateLimit(value = 30, timeUnit = TimeUnit.MINUTES)
    public ResponseEntity<ApiResponseFormat<GetSocialProvidersResponse>> getSocialProviders() {
        final List<SocialProviderInfo> providers = Arrays.stream(SocialProvider.values())
                .map(provider -> SocialProviderInfo.of(
                        provider.name().toLowerCase(Locale.ROOT),
                        provider.name(),
                        OAUTH2_AUTHORIZATION_BASE_URI + provider.name().toLowerCase(Locale.ROOT)
                ))
                .toList();
        return ResponseEntity.ok(ApiResponseFormat.success(
                "지원하는 소셜 로그인 목록을 조회했습니다.", GetSocialProvidersResponse.of(providers)
        ));
    }

    @PostMapping("/refresh")
    @RateLimit(value = 20, timeUnit = TimeUnit.MINUTES)
    public ResponseEntity<ApiResponseFormat<SocialLoginResponse>> refreshToken(
            @CookieValue(name = "refresh_token", required = false) final String refreshToken,
            final HttpServletResponse response
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.MISSING_REFRESH_TOKEN);
        }
        final SocialLoginResponse refreshed = jwtRefreshService.refreshAccessToken(refreshToken);
        jwtCookieHelper.setTokenCookies(response, refreshed);
        log.info("Token refresh successful for user: {}",
                MaskingUtils.maskEmail(refreshed.memberInfo().email()));
        return ResponseEntity.ok(ApiResponseFormat.success("토큰이 갱신되었습니다.", refreshed.redacted()));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponseFormat<Void>> logout(
            @RequestHeader(name = "Authorization", required = false) final String authorization,
            @CookieValue(name = "access_token", required = false) final String accessToken,
            @CookieValue(name = "refresh_token", required = false) final String refreshToken,
            final HttpServletResponse response
    ) {
        final String tokenToLogout = TokenExtractor.extractTokenForLogout(authorization, accessToken);
        try {
            jwtRefreshService.logoutWithTokens(tokenToLogout, refreshToken);
        } finally {
            jwtCookieHelper.clearTokenCookies(response);
        }
        return ResponseEntity.ok(ApiResponseFormat.success("로그아웃되었습니다."));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponseFormat<SocialLoginResponse>> getCurrentAuthStatus(
            @CookieValue(name = "access_token", required = false) final String accessToken
    ) {
        if (accessToken != null && !accessToken.isBlank()) {
            try {
                final TokenValidator.TokenValidationResult validation =
                        tokenValidator.validateAccessToken(accessToken);
                final UserInfoService.UserInfo userInfo =
                        userInfoService.getUserInfo(validation.userId(), validation.userType());
                final SocialLoginResponse authResponse = SocialLoginResponse.of(
                        accessToken,
                        null,
                        accessTokenGenerator.getExpirySeconds(),
                        SocialLoginResponse.MemberInfo.of(
                                validation.userId(), userInfo.email(), null, null, validation.userType()
                        )
                );
                return ResponseEntity.ok(ApiResponseFormat.success(
                        "현재 인증 상태를 조회했습니다.", authResponse.redacted()
                ));
            } catch (final Exception e) {
                log.debug("Access token validation failed: {}", e.getMessage());
            }
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponseFormat.success("인증되지 않은 사용자입니다."));
    }

    private String registrationId(final String provider) {
        try {
            return SocialProvider.valueOf(provider.toUpperCase(Locale.ROOT)).name().toLowerCase(Locale.ROOT);
        } catch (final RuntimeException e) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_SOCIAL_PROVIDER, provider);
        }
    }

    public record SocialProviderInfo(String id, String name, String authUrl) {
        public static SocialProviderInfo of(final String id, final String name, final String authUrl) {
            return new SocialProviderInfo(id, name, authUrl);
        }
    }

    public record GetSocialProvidersResponse(List<SocialProviderInfo> providers) {
        public static GetSocialProvidersResponse of(final List<SocialProviderInfo> providers) {
            return new GetSocialProvidersResponse(providers);
        }
    }
}
