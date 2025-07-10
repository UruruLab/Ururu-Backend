package com.ururulab.ururu.auth.controller;

import com.ururulab.ururu.auth.dto.request.SellerLoginRequest;
import com.ururulab.ururu.auth.dto.response.SocialLoginResponse;
import com.ururulab.ururu.auth.jwt.JwtCookieHelper;
import com.ururulab.ururu.auth.service.SellerAuthService;
import com.ururulab.ururu.auth.service.JwtRefreshService;
import com.ururulab.ururu.global.domain.dto.ApiResponseFormat;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 판매자 인증 관련 API 컨트롤러.
 * 
 * 소셜 로그인과 동일한 JWT 토큰 구조와 쿠키 관리를 사용합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth/seller")
@RequiredArgsConstructor
public class SellerAuthController {

    private static final int SENSITIVE_DATA_PREVIEW_LENGTH = 8;
    private static final String MASKED_DATA_PLACEHOLDER = "***";

    private final SellerAuthService sellerAuthService;
    private final JwtCookieHelper jwtCookieHelper;
    private final JwtRefreshService jwtRefreshService;
    private final Environment environment;

    /**
     * 판매자 로그인 API.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponseFormat<SocialLoginResponse>> login(
            @Valid @RequestBody final SellerLoginRequest request,
            final HttpServletResponse response) {
        
        log.info("Seller login attempt: {}", maskSensitiveData(request.email()));
        
        // 판매자 로그인 처리
        final SocialLoginResponse loginResponse = sellerAuthService.login(request);
        
        // JWT 토큰을 쿠키로 설정
        setSecureCookies(response, loginResponse);
        
        // 보안을 위해 토큰 정보는 마스킹해서 응답
        final SocialLoginResponse secureResponse = createSecureResponse(loginResponse);
        
        log.info("Seller login successful: {} (env: {})", 
                maskSensitiveData(loginResponse.memberInfo().email()), getCurrentProfile());
        
        return ResponseEntity.ok(
                ApiResponseFormat.success("판매자 로그인이 완료되었습니다.", secureResponse)
        );
    }

    /**
     * 판매자 로그아웃 API.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponseFormat<Void>> logout(
            @RequestHeader(name = "Authorization", required = false) final String authorization,
            final HttpServletResponse response) {
        
        if (authorization != null && !authorization.isBlank()) {
            sellerAuthService.logout(authorization);
        }
        
        // 쿠키 삭제
        jwtCookieHelper.clearTokenCookies(response);
        
        log.info("Seller logged out successfully, cookies cleared (env: {})", getCurrentProfile());
        
        return ResponseEntity.ok(
                ApiResponseFormat.success("로그아웃되었습니다.")
        );
    }

    /**
     * 판매자 토큰 갱신 API.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponseFormat<SocialLoginResponse>> refreshToken(
            @CookieValue(name = "refresh_token", required = false) final String refreshToken,
            final HttpServletResponse response) {
        
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.MISSING_REFRESH_TOKEN);
        }
        
        // 토큰 갱신은 공통 JwtRefreshService 사용
        // (판매자와 회원 모두 동일한 토큰 구조 사용)
        final SocialLoginResponse refreshResponse = jwtRefreshService.refreshAccessToken(refreshToken);
        
        // 새로운 토큰을 쿠키로 설정
        jwtCookieHelper.setAccessTokenCookie(response, refreshResponse.accessToken());
        if (refreshResponse.refreshToken() != null) {
            jwtCookieHelper.setRefreshTokenCookie(response, refreshResponse.refreshToken());
        }
        
        // 보안을 위해 토큰 정보는 마스킹해서 응답
        final SocialLoginResponse secureResponse = createSecureResponse(refreshResponse);
        
        log.info("Seller token refresh successful for user: {} (env: {})", 
                maskSensitiveData(refreshResponse.memberInfo().email()), getCurrentProfile());
        
        return ResponseEntity.ok(
                ApiResponseFormat.success("토큰이 갱신되었습니다.", secureResponse)
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

        log.debug("Secure cookies set successfully for seller (env: {})", getCurrentProfile());
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
     * 민감한 데이터 마스킹 (로그용)
     */
    private String maskSensitiveData(final String data) {
        if (data == null || data.length() <= SENSITIVE_DATA_PREVIEW_LENGTH) {
            return MASKED_DATA_PLACEHOLDER;
        }
        return data.substring(0, SENSITIVE_DATA_PREVIEW_LENGTH) + "...";
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