package com.ururulab.ururu.auth.controller;

import com.ururulab.ururu.auth.dto.request.SocialLoginRequest;
import com.ururulab.ururu.auth.dto.response.SocialLoginResponse;
import com.ururulab.ururu.auth.jwt.JwtTokenProvider;
import com.ururulab.ururu.auth.service.SocialLoginService;
import com.ururulab.ururu.auth.service.SocialLoginServiceFactory;
import com.ururulab.ururu.global.common.dto.ApiResponse;
import com.ururulab.ururu.member.domain.entity.enumerated.SocialProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
 * 인증 관련 REST API 컨트롤러.
 *
 * <p>API 명세서에 따른 엔드포인트를 제공합니다.</p>
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public final class AuthController {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder BASE64_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final int SENSITIVE_DATA_PREVIEW_LENGTH = 10;
    private static final String MASKED_DATA_PLACEHOLDER = "***";

    private final SocialLoginServiceFactory socialLoginServiceFactory;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/social/sessions")
    public ResponseEntity<ApiResponse<SocialLoginResponse>> socialLogin(
            @Valid @RequestBody final SocialLoginRequest request
    ) {
        try {
            final SocialLoginService loginService = socialLoginServiceFactory.getService(request.provider());
            final SocialLoginResponse loginResponse = loginService.processLogin(request.code());

            log.info("Social login successful for provider: {}, member: {}",
                    request.provider(), loginResponse.memberInfo().memberId());

            return ResponseEntity.ok(
                    ApiResponse.success("소셜 로그인에 성공했습니다.", loginResponse)
            );

        } catch (final Exception e) {
            log.error("Social login failed for provider: {}", request.provider(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("소셜 로그인에 실패했습니다: " + e.getMessage()));
        }
    }


    @GetMapping("/social/providers")
    public ResponseEntity<ApiResponse<GetSocialProvidersResponse>> getSocialProviders() {
        final List<SocialProviderInfo> providers = Arrays.stream(SocialProvider.values())
                .map(provider -> SocialProviderInfo.of(
                        provider.name().toLowerCase(),
                        provider.name(),
                        generateAuthUrl(provider),
                        generateSecureState()
                ))
                .toList();

        final GetSocialProvidersResponse response = GetSocialProvidersResponse.of(providers);

        return ResponseEntity.ok(
                ApiResponse.success("지원 소셜 로그인 목록을 조회했습니다.", response)
        );
    }

    @GetMapping("/oauth/kakao")
    public RedirectView handleKakaoCallback(
            @RequestParam(required = false) final String code,
            @RequestParam(required = false) final String state,
            @RequestParam(required = false) final String error) {

        log.debug("Kakao callback received: code={}, state={}, error={}",
                maskSensitiveData(code), maskSensitiveData(state), error);

        final RedirectView redirectView = new RedirectView();
        redirectView.setContextRelative(true);

        if (error != null) {
            redirectView.setUrl("/?error=" + URLEncoder.encode(error, StandardCharsets.UTF_8));
            log.warn("Kakao authentication failed: error={}", error);
        } else if (code != null) {
            final StringBuilder url = new StringBuilder("/?code=")
                    .append(URLEncoder.encode(code, StandardCharsets.UTF_8));

            if (state != null) {
                url.append("&state=").append(URLEncoder.encode(state, StandardCharsets.UTF_8));
            }
            redirectView.setUrl(url.toString());
            log.debug("Redirecting to index with auth code");
        } else {
            redirectView.setUrl("/?error=missing_parameters");
            log.warn("Kakao callback missing required parameters");
        }

        return redirectView;
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        log.info("Member logout requested");
        return ResponseEntity.ok(
                ApiResponse.success("로그아웃되었습니다.")
        );
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<AuthStatusResponse>> getAuthStatus(
            @RequestHeader(value = "Authorization", required = false) final String authorization) {

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.ok(
                    ApiResponse.success("인증되지 않은 상태입니다.",
                            AuthStatusResponse.unauthenticated())
            );
        }

        try {
            final String token = authorization.substring(7);
            final Long memberId = jwtTokenProvider.getMemberId(token);
            final String email = jwtTokenProvider.getEmail(token);
            final String role = jwtTokenProvider.getRole(token);

            if (jwtTokenProvider.validateToken(token)) {
                return ResponseEntity.ok(
                        ApiResponse.success("인증된 상태입니다.",
                                AuthStatusResponse.authenticated(memberId, email, role))
                );
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.fail("토큰이 유효하지 않습니다."));
            }
        } catch (final Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail("토큰 검증에 실패했습니다."));
        }
    }

    private String generateSecureState() {
        final byte[] randomBytes = new byte[32]; // 256 bits
        SECURE_RANDOM.nextBytes(randomBytes);
        return BASE64_ENCODER.encodeToString(randomBytes);
    }

    private String generateAuthUrl(final SocialProvider provider) {
        try {
            final SocialLoginService loginService = socialLoginServiceFactory.getService(provider);
            final String state = generateSecureState();
            return loginService.getAuthorizationUrl(state);
        } catch (final Exception e) {
            log.warn("Failed to generate auth URL for provider: {}", provider, e);
            return "";
        }
    }

    private String maskSensitiveData(final String data) {
        if (data == null || data.length() <= SENSITIVE_DATA_PREVIEW_LENGTH) {
            return MASKED_DATA_PLACEHOLDER;
        }
        return data.substring(0, SENSITIVE_DATA_PREVIEW_LENGTH) + "...";
    }

    public record GetSocialProvidersResponse(
            List<SocialProviderInfo> providers
    ) {
        public static GetSocialProvidersResponse of(final List<SocialProviderInfo> providers) {
            if (providers == null) {
                throw new IllegalArgumentException("소셜 제공자 목록은 필수입니다.");
            }
            return new GetSocialProvidersResponse(providers);
        }
    }

    public record SocialProviderInfo(
            String id,          // "kakao", "google"
            String displayName, // "KAKAO", "GOOGLE"
            String authUrl,     // 인증 URL
            String state        // CSRF 방지용 state
    ) {
        public static SocialProviderInfo of(
                final String id,
                final String displayName,
                final String authUrl,
                final String state
        ) {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("소셜 제공자 ID는 필수입니다.");
            }
            if (displayName == null || displayName.isBlank()) {
                throw new IllegalArgumentException("소셜 제공자 표시명은 필수입니다.");
            }
            return new SocialProviderInfo(id, displayName, authUrl, state);
        }
    }

    public record AuthStatusResponse(
            boolean authenticated,
            Long memberId,
            String email,
            String role,
            String message
    ) {
        public static AuthStatusResponse authenticated(
                final Long memberId,
                final String email,
                final String role) {
            return new AuthStatusResponse(true, memberId, email, role, "인증됨");
        }

        public static AuthStatusResponse unauthenticated() {
            return new AuthStatusResponse(false, null, null, null, "인증되지 않음");
        }
    }
}