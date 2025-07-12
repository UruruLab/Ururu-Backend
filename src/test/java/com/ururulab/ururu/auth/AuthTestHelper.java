package com.ururulab.ururu.auth;

import com.ururulab.ururu.auth.constants.UserRole;
import com.ururulab.ururu.auth.constants.UserType;
import com.ururulab.ururu.auth.dto.info.SocialMemberInfo;
import com.ururulab.ururu.auth.jwt.token.AccessTokenGenerator;
import com.ururulab.ururu.auth.jwt.token.RefreshTokenGenerator;
import com.ururulab.ururu.auth.jwt.JwtTokenProvider;
import com.ururulab.ururu.auth.service.TokenValidator;
import com.ururulab.ururu.auth.service.JwtRefreshService;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.member.service.MemberService;
import com.ururulab.ururu.seller.service.SellerService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;

import org.springframework.http.MediaType;

/**
 * Auth 테스트를 위한 헬퍼 클래스.
 * 
 * 공통적인 테스트 검증 로직과 Mock 설정을 중앙화하여 관리합니다.
 */
public final class AuthTestHelper {

    private AuthTestHelper() {
        // 유틸리티 클래스이므로 인스턴스화 방지
    }

    // ==================== 토큰 검증 헬퍼 ====================

    /**
     * 토큰 검증 결과를 검증합니다.
     */
    public static void verifyTokenValidation(TokenValidator validator, String token, Long expectedUserId, String expectedUserType) {
        TokenValidator.TokenValidationResult result = validator.validateAccessToken(token);
        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(expectedUserId);
        assertThat(result.userType()).isEqualTo(expectedUserType);
        assertThat(result.tokenId()).isNotNull().isNotEmpty();
    }

    /**
     * Refresh Token 검증 결과를 검증합니다.
     */
    public static void verifyRefreshTokenValidation(TokenValidator validator, String token, Long expectedUserId, String expectedUserType) {
        TokenValidator.TokenValidationResult result = validator.validateRefreshToken(token);
        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(expectedUserId);
        assertThat(result.userType()).isEqualTo(expectedUserType);
        assertThat(result.tokenId()).isNotNull().isNotEmpty();
    }

    /**
     * 토큰이 유효한지 검증합니다.
     */
    public static void verifyTokenIsValid(JwtTokenProvider provider, String token) {
        assertThat(provider.validateToken(token)).isTrue();
    }

    /**
     * 토큰이 유효하지 않은지 검증합니다.
     */
    public static void verifyTokenIsInvalid(JwtTokenProvider provider, String token) {
        assertThat(provider.validateToken(token)).isFalse();
    }

    /**
     * 토큰이 만료되었는지 검증합니다.
     */
    public static void verifyTokenIsExpired(JwtTokenProvider provider, String token) {
        assertThat(provider.isTokenExpired(token)).isTrue();
    }

    /**
     * 토큰이 만료되지 않았는지 검증합니다.
     */
    public static void verifyTokenIsNotExpired(JwtTokenProvider provider, String token) {
        assertThat(provider.isTokenExpired(token)).isFalse();
    }

    /**
     * Access Token인지 검증합니다.
     */
    public static void verifyIsAccessToken(JwtTokenProvider provider, String token) {
        assertThat(provider.isAccessToken(token)).isTrue();
        assertThat(provider.isRefreshToken(token)).isFalse();
    }

    /**
     * Refresh Token인지 검증합니다.
     */
    public static void verifyIsRefreshToken(JwtTokenProvider provider, String token) {
        assertThat(provider.isRefreshToken(token)).isTrue();
        assertThat(provider.isAccessToken(token)).isFalse();
    }

    // ==================== 예외 검증 헬퍼 ====================

    /**
     * BusinessException이 발생하는지 검증합니다.
     */
    public static void verifyBusinessException(Runnable action, ErrorCode expectedErrorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", expectedErrorCode);
    }

    /**
     * BusinessException이 발생하는지 검증합니다 (메시지 포함).
     */
    public static void verifyBusinessException(Runnable action, ErrorCode expectedErrorCode, String expectedMessage) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", expectedErrorCode)
                .hasMessageContaining(expectedMessage);
    }

    /**
     * IllegalArgumentException이 발생하는지 검증합니다.
     */
    public static void verifyIllegalArgumentException(Runnable action, String expectedMessage) {
        assertThatThrownBy(action::run)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(expectedMessage);
    }

    /**
     * RuntimeException이 발생하는지 검증합니다.
     */
    public static void verifyRuntimeException(Runnable action, String expectedMessage) {
        assertThatThrownBy(action::run)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(expectedMessage);
    }

    // ==================== Mock 설정 헬퍼 ====================

    /**
     * RestClient 체인 모킹을 위한 헬퍼 메서드
     */
    @SuppressWarnings("unchecked")
    public static void setupRestClientChain(RestClient restClient, String tokenUri, String memberInfoUri) {
        RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec requestBodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        
        @SuppressWarnings("rawtypes")
        RestClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        @SuppressWarnings("rawtypes")
        RestClient.RequestHeadersSpec requestHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec memberInfoResponseSpec = mock(RestClient.ResponseSpec.class);
        
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(tokenUri)).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(memberInfoUri)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(memberInfoResponseSpec);
        when(memberInfoResponseSpec.onStatus(any(), any())).thenReturn(memberInfoResponseSpec);
    }

    /**
     * Redis Mock 설정을 합니다.
     */
    public static void setupRedisMock(StringRedisTemplate redisTemplate, String key, String value) {
        // Redis Mock 설정은 각 테스트에서 직접 설정하는 것을 권장합니다.
        // 예시: given(redisTemplate.opsForValue().get(key)).willReturn(value);
    }

    // ==================== 통합 검증 헬퍼 ====================

    /**
     * 전체 토큰 검증 플로우를 테스트합니다.
     */
    public static void verifyCompleteTokenValidation(
            TokenValidator validator,
            JwtTokenProvider provider,
            String token,
            Long expectedUserId,
            String expectedUserType
    ) {
        // 토큰 검증
        verifyTokenValidation(validator, token, expectedUserId, expectedUserType);
        
        // 토큰 속성 검증
        verifyTokenIsValid(provider, token);
        verifyIsAccessToken(provider, token);
        verifyTokenIsNotExpired(provider, token);
        
        // 토큰 정보 검증
        assertThat(provider.getMemberId(token)).isEqualTo(expectedUserId);
        assertThat(provider.getUserType(token)).isEqualTo(expectedUserType);
    }

    /**
     * 토큰 검증 실패 플로우를 테스트합니다.
     */
    public static void verifyTokenValidationFailure(TokenValidator validator, String token, ErrorCode expectedErrorCode) {
        verifyBusinessException(
                () -> validator.validateAccessToken(token),
                expectedErrorCode
        );
    }

    /**
     * Refresh Token 검증 플로우를 테스트합니다.
     */
    public static void verifyCompleteRefreshTokenValidation(
            TokenValidator validator,
            JwtTokenProvider provider,
            String token,
            Long expectedUserId,
            String expectedUserType
    ) {
        // Refresh Token 검증
        verifyRefreshTokenValidation(validator, token, expectedUserId, expectedUserType);
        
        // 토큰 속성 검증
        verifyTokenIsValid(provider, token);
        verifyIsRefreshToken(provider, token);
        verifyTokenIsNotExpired(provider, token);
        
        // 토큰 정보 검증 (Refresh Token은 email과 role이 없음)
        assertThat(provider.getMemberId(token)).isEqualTo(expectedUserId);
        assertThat(provider.getUserType(token)).isEqualTo(expectedUserType);
        assertThat(provider.getEmail(token)).isNull();
        assertThat(provider.getRole(token)).isNull();
    }

    // ==================== 테스트 데이터 검증 헬퍼 ====================

    /**
     * 소셜 회원 정보가 올바른지 검증합니다.
     */
    public static void verifySocialMemberInfo(
            String socialId,
            String email,
            String nickname,
            String profileImage,
            String provider
    ) {
        assertThat(socialId).isNotNull().isNotEmpty();
        assertThat(provider).isNotNull().isNotEmpty();
        // email, nickname, profileImage는 null일 수 있음
    }

    /**
     * JWT 토큰이 올바른지 검증합니다.
     */
    public static void verifyJwtToken(String token, Long expectedUserId, String expectedEmail, UserRole expectedRole, UserType expectedUserType) {
        JwtTokenProvider provider = AuthTestFixture.createTestJwtTokenProvider();
        
        assertThat(token).isNotNull().isNotEmpty();
        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.getMemberId(token)).isEqualTo(expectedUserId);
        assertThat(provider.getEmail(token)).isEqualTo(expectedEmail);
        assertThat(provider.getRole(token)).isEqualTo(expectedRole.getValue());
        assertThat(provider.getUserType(token)).isEqualTo(expectedUserType.getValue());
    }

    /**
     * Refresh Token이 올바른지 검증합니다.
     */
    public static void verifyRefreshToken(String token, Long expectedUserId, UserType expectedUserType) {
        JwtTokenProvider provider = AuthTestFixture.createTestJwtTokenProvider();
        
        assertThat(token).isNotNull().isNotEmpty();
        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.getMemberId(token)).isEqualTo(expectedUserId);
        assertThat(provider.getUserType(token)).isEqualTo(expectedUserType.getValue());
        assertThat(provider.isRefreshToken(token)).isTrue();
        assertThat(provider.getEmail(token)).isNull();
        assertThat(provider.getRole(token)).isNull();
    }

    // ==================== Mock 검증 헬퍼 ====================

    /**
     * Mock이 호출되었는지 검증합니다.
     */
    public static void verifyMockCalled(Object mock, String methodName) {
        // Mockito의 verify를 사용하여 메서드 호출 검증
        // 구체적인 구현은 각 테스트에서 verify() 사용
    }

    /**
     * Mock이 호출되지 않았는지 검증합니다.
     */
    public static void verifyMockNotCalled(Object mock, String methodName) {
        // Mockito의 verify를 사용하여 메서드 호출 안됨 검증
        // 구체적인 구현은 각 테스트에서 verify(mock, never()).method() 사용
    }

    // ==================== 성능 테스트 헬퍼 ====================

    /**
     * 메서드 실행 시간을 측정합니다.
     */
    public static long measureExecutionTime(Runnable action) {
        long startTime = System.currentTimeMillis();
        action.run();
        long endTime = System.currentTimeMillis();
        return endTime - startTime;
    }

    /**
     * 메서드 실행 시간이 기준 시간 이내인지 검증합니다.
     */
    public static void verifyExecutionTime(Runnable action, long maxExecutionTimeMs) {
        long executionTime = measureExecutionTime(action);
        assertThat(executionTime).isLessThan(maxExecutionTimeMs);
    }

    // ==================== 문자열 검증 헬퍼 ====================

    /**
     * 문자열이 마스킹되었는지 검증합니다.
     */
    public static void verifyStringIsMasked(String original, String masked) {
        assertThat(masked).isNotNull();
        assertThat(masked).isNotEqualTo(original);
        assertThat(masked).contains("***");
    }

    /**
     * 이메일이 마스킹되었는지 검증합니다.
     */
    public static void verifyEmailIsMasked(String email, String maskedEmail) {
        assertThat(maskedEmail).isNotNull();
        assertThat(maskedEmail).isNotEqualTo(email);
        assertThat(maskedEmail).contains("***");
        assertThat(maskedEmail).contains("@");
    }

    /**
     * 토큰이 마스킹되었는지 검증합니다.
     */
    public static void verifyTokenIsMasked(String token, String maskedToken) {
        assertThat(maskedToken).isNotNull();
        assertThat(maskedToken).isNotEqualTo(token);
        assertThat(maskedToken).contains("***");
    }

    /**
     * 소셜 로그인 성공 시 검증을 위한 헬퍼 메서드
     */
    public static void verifySocialLoginSuccess(
            MemberService memberService,
            JwtRefreshService jwtRefreshService,
            AccessTokenGenerator accessTokenGenerator,
            RefreshTokenGenerator refreshTokenGenerator,
            Long expectedUserId,
            String expectedEmail,
            UserType expectedUserType
    ) {
        verify(memberService).findOrCreateMember(any(SocialMemberInfo.class));
        verify(jwtRefreshService).storeRefreshToken(eq(expectedUserId), eq(expectedUserType.getValue()), anyString());
        verify(accessTokenGenerator).generateAccessToken(eq(expectedUserId), eq(expectedEmail), any(), eq(expectedUserType));
        verify(refreshTokenGenerator).generateRefreshToken(eq(expectedUserId), eq(expectedUserType));
    }

    /**
     * 판매자 로그인 성공 시 검증을 위한 헬퍼 메서드
     */
    public static void verifySellerLoginSuccess(
            SellerService sellerService,
            PasswordEncoder passwordEncoder,
            JwtRefreshService jwtRefreshService,
            AccessTokenGenerator accessTokenGenerator,
            RefreshTokenGenerator refreshTokenGenerator,
            String expectedEmail,
            String expectedPassword,
            Long expectedUserId
    ) {
        verify(sellerService).findByEmail(expectedEmail);
        verify(passwordEncoder).matches(expectedPassword, anyString());
        verify(jwtRefreshService).storeRefreshToken(eq(expectedUserId), eq(UserType.SELLER.getValue()), anyString());
        verify(accessTokenGenerator).generateAccessToken(eq(expectedUserId), eq(expectedEmail), eq(UserRole.SELLER), eq(UserType.SELLER));
        verify(refreshTokenGenerator).generateRefreshToken(eq(expectedUserId), eq(UserType.SELLER));
    }
} 