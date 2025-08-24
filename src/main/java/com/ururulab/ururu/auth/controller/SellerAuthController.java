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
import com.ururulab.ururu.auth.util.EnvironmentHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.ururulab.ururu.auth.service.CsrfTokenService;

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
    private final SecurityLoggingService securityLoggingService;
    private final EnvironmentHelper environmentHelper;
    private final CsrfTokenService csrfTokenService;

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
        AuthCookieHelper.setSecureCookies(response, loginResponse, jwtCookieHelper);
        
        // CSRF 토큰 생성 및 응답 헤더에 추가
        final String csrfToken = csrfTokenService.generateCsrfToken(
                loginResponse.memberInfo().memberId(), 
                loginResponse.memberInfo().userType()
        );
        response.setHeader("X-CSRF-TOKEN", csrfToken);
        
        // 보안을 위해 토큰 정보는 마스킹해서 응답
        final SocialLoginResponse secureResponse = AuthResponseHelper.createSecureResponse(loginResponse, securityLoggingService);
        
        log.info("Seller login successful: {} (env: {})", 
                securityLoggingService.maskEmail(loginResponse.memberInfo().email()), environmentHelper.getCurrentProfile());
        
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
        
        log.info("Seller logged out successfully, cookies cleared (env: {})", environmentHelper.getCurrentProfile());
        
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
        
        if (!TokenExtractor.isValidRefreshToken(refreshToken)) {
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
        
        // 새로운 CSRF 토큰 생성 및 응답 헤더에 추가
        final String newCsrfToken = jwtRefreshService.generateNewCsrfToken(
                refreshResponse.memberInfo().memberId(), 
                refreshResponse.memberInfo().userType()
        );
        response.setHeader("X-CSRF-TOKEN", newCsrfToken);
        
        // 보안을 위해 토큰 정보는 마스킹해서 응답
        final SocialLoginResponse secureResponse = AuthResponseHelper.createSecureResponse(refreshResponse, securityLoggingService);
        
        log.info("Seller token refresh successful for user: {} (env: {})", 
                MaskingUtils.maskEmail(refreshResponse.memberInfo().email()), environmentHelper.getCurrentProfile());
        
        return ResponseEntity.ok(
                ApiResponseFormat.success("토큰이 갱신되었습니다.", secureResponse)
        );
    }

    // Private Helper Methods






} 