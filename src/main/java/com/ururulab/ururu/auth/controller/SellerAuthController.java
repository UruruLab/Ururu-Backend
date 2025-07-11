package com.ururulab.ururu.auth.controller;

import com.ururulab.ururu.auth.dto.request.SellerLoginRequest;
import com.ururulab.ururu.auth.dto.response.SocialLoginResponse;
import com.ururulab.ururu.auth.jwt.JwtCookieHelper;
import com.ururulab.ururu.auth.service.SellerAuthService;
import com.ururulab.ururu.auth.service.JwtRefreshService;
import com.ururulab.ururu.auth.util.TokenExtractor;
import com.ururulab.ururu.auth.service.SecurityLoggingService;
import com.ururulab.ururu.auth.util.AuthResponseHelper;
import com.ururulab.ururu.auth.util.AuthCookieHelper;
import com.ururulab.ururu.global.domain.dto.ApiResponseFormat;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.global.util.MaskingUtils;
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

    private final SellerAuthService sellerAuthService;
    private final JwtCookieHelper jwtCookieHelper;
    private final JwtRefreshService jwtRefreshService;
    private final Environment environment;
    private final SecurityLoggingService securityLoggingService;

    /**
     * 판매자 로그인 API.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponseFormat<SocialLoginResponse>> login(
            @Valid @RequestBody final SellerLoginRequest request,
            final HttpServletResponse response) {
        
        log.info("Seller login attempt: {}", securityLoggingService.maskEmail(request.email()));
        
        // 판매자 로그인 처리
        final SocialLoginResponse loginResponse = sellerAuthService.login(request);
        
        // JWT 토큰을 쿠키로 설정
        setSecureCookies(response, loginResponse);
        
        // 보안을 위해 토큰 정보는 마스킹해서 응답
        final SocialLoginResponse secureResponse = createSecureResponse(loginResponse);
        
        log.info("Seller login successful: {} (env: {})", 
                securityLoggingService.maskEmail(loginResponse.memberInfo().email()), getCurrentProfile());
        
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
            @CookieValue(name = "access_token", required = false) final String accessToken,
            @CookieValue(name = "refresh_token", required = false) final String refreshToken,
            final HttpServletResponse response) {
        
        // TokenExtractor를 사용하여 토큰 추출
        final String tokenToLogout = TokenExtractor.extractTokenForLogout(authorization, accessToken);
        
        if (tokenToLogout != null) {
            try {
                sellerAuthService.logoutWithToken(tokenToLogout);
            } catch (final Exception e) {
                log.warn("Failed to logout seller from Redis: {}", e.getMessage());
                // Redis 삭제 실패는 로그아웃을 중단시키지 않음
            }
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
                MaskingUtils.maskEmail(refreshResponse.memberInfo().email()), getCurrentProfile());
        
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
        AuthCookieHelper.setSecureCookies(response, loginResponse, jwtCookieHelper);
        log.debug("Secure cookies set successfully for seller (env: {})", getCurrentProfile());
    }

    /**
     * 보안을 위해 토큰 정보를 마스킹한 응답 생성
     */
    private SocialLoginResponse createSecureResponse(final SocialLoginResponse original) {
        return AuthResponseHelper.createSecureResponse(original, securityLoggingService);
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