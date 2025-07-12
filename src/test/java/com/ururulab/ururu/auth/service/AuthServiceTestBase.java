package com.ururulab.ururu.auth.service;

import com.ururulab.ururu.auth.AuthTestFixture;
import com.ururulab.ururu.auth.jwt.JwtProperties;
import com.ururulab.ururu.auth.jwt.JwtTokenProvider;
import com.ururulab.ururu.auth.storage.RefreshTokenStorage;
import com.ururulab.ururu.auth.storage.TokenBlacklistStorage;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.repository.MemberRepository;
import com.ururulab.ururu.seller.domain.entity.Seller;
import com.ururulab.ururu.seller.domain.repository.SellerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

/**
 * Auth 관련 테스트의 기본 클래스.
 * 
 * Repository 계층만 Mock 처리하고 나머지는 실제 객체를 사용하여
 * 비즈니스 로직을 실제 동작으로 검증할 수 있도록 합니다.
 */
@ExtendWith(MockitoExtension.class)
public abstract class AuthServiceTestBase {

    // ==================== Repository Mock (4개 Repository만 Mock 처리) ====================
    
    @Mock
    protected MemberRepository memberRepository;
    
    @Mock
    protected SellerRepository sellerRepository;
    
    @Mock
    protected RefreshTokenStorage refreshTokenStorage;
    
    @Mock
    protected TokenBlacklistStorage tokenBlacklistStorage;

    // ==================== 실제 Service 객체들 (Mock 처리하지 않음) ====================
    
    protected JwtTokenProvider jwtTokenProvider;
    
    protected JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        // 테스트용 JWT 설정 초기화
        jwtProperties = AuthTestFixture.createTestJwtProperties();
        jwtTokenProvider = AuthTestFixture.createTestJwtTokenProvider();
        
        // Repository Mock 기본 동작 설정
        setupRepositoryMocks();
    }

    /**
     * Repository Mock의 기본 동작을 설정합니다.
     * 각 테스트 클래스에서 필요에 따라 오버라이드할 수 있습니다.
     */
    protected void setupRepositoryMocks() {
        // 기본적인 Mock 동작 설정
        // 구체적인 설정은 각 테스트 클래스에서 구현
    }

    // ==================== Given 메서드들 (테스트 데이터 설정) ====================

    /**
     * 테스트용 Member를 생성하고 Repository Mock에 설정합니다.
     */
    protected void givenMemberExists(Long memberId, String nickname, String email) {
        Member member = AuthTestFixture.createMember(memberId, nickname, email);
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(memberRepository.existsByEmail(email)).willReturn(true);
    }

    /**
     * 테스트용 삭제된 Member를 생성하고 Repository Mock에 설정합니다.
     */
    protected void givenDeletedMemberExists(Long memberId, String nickname, String email) {
        Member member = AuthTestFixture.createDeletedMember(memberId, nickname, email);
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(memberRepository.existsByEmail(email)).willReturn(true);
    }

    /**
     * 테스트용 Seller를 생성하고 Repository Mock에 설정합니다.
     */
    protected void givenSellerExists(Long sellerId, String email, String name) {
        Seller seller = AuthTestFixture.createSeller(sellerId, email, name);
        given(sellerRepository.findActiveSeller(sellerId)).willReturn(Optional.of(seller));
        given(sellerRepository.findByEmail(email)).willReturn(Optional.of(seller));
    }

    /**
     * 테스트용 삭제된 Seller를 생성하고 Repository Mock에 설정합니다.
     */
    protected void givenDeletedSellerExists(Long sellerId, String email, String name) {
        Seller seller = AuthTestFixture.createDeletedSeller(sellerId, email, name);
        given(sellerRepository.findActiveSeller(sellerId)).willReturn(Optional.empty());
        given(sellerRepository.findByEmail(email)).willReturn(Optional.of(seller));
    }

    /**
     * 테스트용 Refresh Token을 Storage Mock에 설정합니다.
     */
    protected void givenRefreshTokenExists(String userType, Long userId, String tokenId, String refreshToken) {
        given(refreshTokenStorage.getRefreshToken(userType, userId, tokenId)).willReturn(refreshToken);
        given(refreshTokenStorage.getRefreshTokenCount(userType, userId)).willReturn(1L);
        given(refreshTokenStorage.isRefreshTokenLimitExceeded(userType, userId)).willReturn(false);
    }

    /**
     * 테스트용 토큰을 Blacklist Mock에 설정합니다.
     */
    protected void givenTokenIsBlacklisted(String tokenId) {
        given(tokenBlacklistStorage.isTokenBlacklisted(tokenId)).willReturn(true);
    }

    /**
     * 테스트용 토큰이 Blacklist에 없음을 Mock에 설정합니다.
     */
    protected void givenTokenIsNotBlacklisted(String tokenId) {
        // lenient를 사용하여 불필요한 stubbing 경고 방지
        lenient().when(tokenBlacklistStorage.isTokenBlacklisted(tokenId)).thenReturn(false);
    }

    /**
     * 테스트용 Refresh Token 개수 제한 초과를 Mock에 설정합니다.
     */
    protected void givenRefreshTokenLimitExceeded(String userType, Long userId) {
        given(refreshTokenStorage.isRefreshTokenLimitExceeded(userType, userId)).willReturn(true);
        given(refreshTokenStorage.getRefreshTokenCount(userType, userId)).willReturn(6L); // 기본 제한 5개 초과
    }

    // ==================== When 메서드들 (테스트 액션) ====================

    /**
     * 토큰 검증 액션을 수행합니다.
     */
    protected TokenValidator.TokenValidationResult whenValidateAccessToken(TokenValidator validator, String token) {
        return validator.validateAccessToken(token);
    }

    /**
     * Refresh Token 검증 액션을 수행합니다.
     */
    protected TokenValidator.TokenValidationResult whenValidateRefreshToken(TokenValidator validator, String token) {
        return validator.validateRefreshToken(token);
    }

    // ==================== Then 메서드들 (검증 헬퍼) ====================

    /**
     * 토큰 검증 결과를 검증합니다.
     */
    protected void thenTokenValidationShouldSucceed(TokenValidator.TokenValidationResult result, Long expectedUserId, String expectedUserType) {
        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(expectedUserId);
        assertThat(result.userType()).isEqualTo(expectedUserType);
        assertThat(result.tokenId()).isNotNull().isNotEmpty();
    }

    /**
     * BusinessException이 발생하는지 검증합니다.
     */
    protected void thenBusinessExceptionShouldBeThrown(Runnable action, ErrorCode expectedErrorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", expectedErrorCode);
    }

    /**
     * BusinessException이 발생하는지 검증합니다 (메시지 포함).
     */
    protected void thenBusinessExceptionShouldBeThrown(Runnable action, ErrorCode expectedErrorCode, String expectedMessage) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", expectedErrorCode)
                .hasMessageContaining(expectedMessage);
    }

    /**
     * IllegalArgumentException이 발생하는지 검증합니다.
     */
    protected void thenIllegalArgumentExceptionShouldBeThrown(Runnable action, String expectedMessage) {
        assertThatThrownBy(action::run)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(expectedMessage);
    }

    /**
     * 토큰이 유효한지 검증합니다.
     */
    protected void thenTokenShouldBeValid(String token) {
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    /**
     * 토큰이 유효하지 않은지 검증합니다.
     */
    protected void thenTokenShouldBeInvalid(String token) {
        assertThat(jwtTokenProvider.validateToken(token)).isFalse();
    }

    /**
     * 토큰이 만료되었는지 검증합니다.
     */
    protected void thenTokenShouldBeExpired(String token) {
        assertThat(jwtTokenProvider.isTokenExpired(token)).isTrue();
    }

    /**
     * 토큰이 만료되지 않았는지 검증합니다.
     */
    protected void thenTokenShouldNotBeExpired(String token) {
        assertThat(jwtTokenProvider.isTokenExpired(token)).isFalse();
    }

    /**
     * Access Token인지 검증합니다.
     */
    protected void thenShouldBeAccessToken(String token) {
        assertThat(jwtTokenProvider.isAccessToken(token)).isTrue();
        assertThat(jwtTokenProvider.isRefreshToken(token)).isFalse();
    }

    /**
     * Refresh Token인지 검증합니다.
     */
    protected void thenShouldBeRefreshToken(String token) {
        assertThat(jwtTokenProvider.isRefreshToken(token)).isTrue();
        assertThat(jwtTokenProvider.isAccessToken(token)).isFalse();
    }

    // ==================== 통합 헬퍼 메서드들 ====================

    /**
     * 전체 토큰 검증 플로우를 테스트합니다.
     */
    protected void verifyCompleteTokenValidation(TokenValidator validator, String token, Long expectedUserId, String expectedUserType) {
        // Given
        String tokenId = jwtTokenProvider.getTokenId(token);
        givenTokenIsNotBlacklisted(tokenId);

        // When
        TokenValidator.TokenValidationResult result = whenValidateAccessToken(validator, token);

        // Then
        thenTokenValidationShouldSucceed(result, expectedUserId, expectedUserType);
        thenTokenShouldBeValid(token);
        thenShouldBeAccessToken(token);
        thenTokenShouldNotBeExpired(token);
    }

    /**
     * 토큰 검증 실패 플로우를 테스트합니다.
     */
    protected void verifyTokenValidationFailure(TokenValidator validator, String token, ErrorCode expectedErrorCode) {
        // When & Then
        thenBusinessExceptionShouldBeThrown(
                () -> whenValidateAccessToken(validator, token),
                expectedErrorCode
        );
    }

    /**
     * Refresh Token 검증 플로우를 테스트합니다.
     */
    protected void verifyRefreshTokenValidation(TokenValidator validator, String token, Long expectedUserId, String expectedUserType) {
        // Given
        String tokenId = jwtTokenProvider.getTokenId(token);
        givenTokenIsNotBlacklisted(tokenId);

        // When
        TokenValidator.TokenValidationResult result = whenValidateRefreshToken(validator, token);

        // Then
        thenTokenValidationShouldSucceed(result, expectedUserId, expectedUserType);
        thenTokenShouldBeValid(token);
        thenShouldBeRefreshToken(token);
        thenTokenShouldNotBeExpired(token);
    }

    /**
     * 전체 Refresh Token 검증 플로우를 테스트합니다.
     */
    protected void verifyCompleteRefreshTokenValidation(TokenValidator validator, String token, Long expectedUserId, String expectedUserType) {
        // Given
        String tokenId = jwtTokenProvider.getTokenId(token);
        givenTokenIsNotBlacklisted(tokenId);

        // When
        TokenValidator.TokenValidationResult result = whenValidateRefreshToken(validator, token);

        // Then
        thenTokenValidationShouldSucceed(result, expectedUserId, expectedUserType);
        thenTokenShouldBeValid(token);
        thenShouldBeRefreshToken(token);
        thenTokenShouldNotBeExpired(token);
    }

    // ==================== Mock 검증 헬퍼 메서드들 ====================

    /**
     * Refresh Token이 저장되었는지 검증합니다.
     */
    protected void thenRefreshTokenShouldBeStored(String userType, Long userId, String refreshToken) {
        verify(refreshTokenStorage).storeRefreshToken(userId, userType, refreshToken);
    }

    /**
     * Refresh Token이 삭제되었는지 검증합니다.
     */
    protected void thenRefreshTokenShouldBeDeleted(String userType, Long userId, String tokenId) {
        verify(refreshTokenStorage).deleteRefreshToken(userType, userId, tokenId);
    }

    /**
     * 모든 Refresh Token이 삭제되었는지 검증합니다.
     */
    protected void thenAllRefreshTokensShouldBeDeleted(String userType, Long userId) {
        verify(refreshTokenStorage).deleteAllRefreshTokens(userType, userId);
    }

    /**
     * 토큰이 블랙리스트에 추가되었는지 검증합니다.
     */
    protected void thenTokenShouldBeBlacklisted(String tokenId) {
        verify(tokenBlacklistStorage).addToBlacklist(tokenId, any(Long.class));
    }

    /**
     * Access Token이 블랙리스트에 추가되었는지 검증합니다.
     */
    protected void thenAccessTokenShouldBeBlacklisted(String accessToken) {
        verify(tokenBlacklistStorage).blacklistAccessToken(accessToken);
    }

    /**
     * Refresh Token이 블랙리스트에 추가되었는지 검증합니다.
     */
    protected void thenRefreshTokenShouldBeBlacklisted(String refreshToken) {
        verify(tokenBlacklistStorage).blacklistRefreshToken(refreshToken);
    }
} 