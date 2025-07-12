package com.ururulab.ururu.auth.service;

import com.ururulab.ururu.auth.AuthTestFixture;
import com.ururulab.ururu.auth.constants.UserRole;
import com.ururulab.ururu.auth.constants.UserType;
import com.ururulab.ururu.auth.jwt.JwtTokenProvider;
import com.ururulab.ururu.auth.storage.TokenBlacklistStorage;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/**
 * TokenValidator 테스트.
 * 
 * JWT 토큰의 유효성 검사, 사용자 정보 추출, 블랙리스트 확인 등을 통합 테스트합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TokenValidator 테스트")
class TokenValidatorTest {

    @Mock
    private TokenBlacklistStorage tokenBlacklistStorage;

    private JwtTokenProvider jwtTokenProvider;
    private TokenValidator tokenValidator;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = AuthTestFixture.createTestJwtTokenProvider();
        tokenValidator = new TokenValidator(jwtTokenProvider, tokenBlacklistStorage);
    }

    // ==================== Access Token 검증 테스트 ====================

    @Test
    @DisplayName("유효한 Access Token 검증 성공")
    void validateAccessToken_validToken_success() {
        // Given
        String validToken = AuthTestFixture.createValidAccessToken(1L, "test@example.com", UserRole.NORMAL, UserType.MEMBER);
        String tokenId = jwtTokenProvider.getTokenId(validToken);
        
        given(tokenBlacklistStorage.isTokenBlacklisted(tokenId)).willReturn(false);

        // When
        TokenValidator.TokenValidationResult result = tokenValidator.validateAccessToken(validToken);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.userType()).isEqualTo(UserType.MEMBER.getValue());
        assertThat(result.tokenId()).isEqualTo(tokenId);
    }

    @Test
    @DisplayName("판매자 Access Token 검증 성공")
    void validateAccessToken_sellerToken_success() {
        // Given
        String sellerToken = AuthTestFixture.createValidAccessToken(1L, "seller@example.com", UserRole.SELLER, UserType.SELLER);
        String tokenId = jwtTokenProvider.getTokenId(sellerToken);
        
        given(tokenBlacklistStorage.isTokenBlacklisted(tokenId)).willReturn(false);

        // When
        TokenValidator.TokenValidationResult result = tokenValidator.validateAccessToken(sellerToken);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.userType()).isEqualTo(UserType.SELLER.getValue());
        assertThat(result.tokenId()).isEqualTo(tokenId);
    }

    @Test
    @DisplayName("null Access Token 검증 시 예외")
    void validateAccessToken_nullToken_throwsException() {
        // Given
        String nullToken = AuthTestFixture.createNullToken();

        // When & Then
        assertThatThrownBy(() -> tokenValidator.validateAccessToken(nullToken))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MISSING_AUTHORIZATION_HEADER);
    }

    @Test
    @DisplayName("빈 Access Token 검증 시 예외")
    void validateAccessToken_emptyToken_throwsException() {
        // Given
        String emptyToken = AuthTestFixture.createEmptyToken();

        // When & Then
        assertThatThrownBy(() -> tokenValidator.validateAccessToken(emptyToken))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MISSING_AUTHORIZATION_HEADER);
    }

    @Test
    @DisplayName("잘못된 형식의 Access Token 검증 시 예외")
    void validateAccessToken_invalidToken_throwsException() {
        // Given
        String invalidToken = AuthTestFixture.createInvalidToken();

        // When & Then
        assertThatThrownBy(() -> tokenValidator.validateAccessToken(invalidToken))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_JWT_TOKEN);
    }

    @Test
    @DisplayName("Refresh Token을 Access Token으로 검증 시 예외")
    void validateAccessToken_refreshToken_throwsException() {
        // Given
        String refreshToken = AuthTestFixture.createValidRefreshToken(1L, UserType.MEMBER);

        // When & Then
        assertThatThrownBy(() -> tokenValidator.validateAccessToken(refreshToken))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_JWT_TOKEN);
    }

    @Test
    @DisplayName("만료된 Access Token 검증 시 예외")
    void validateAccessToken_expiredToken_throwsException() {
        // Given
        String expiredToken = AuthTestFixture.createExpiredAccessToken(1L, "test@example.com", UserRole.NORMAL, UserType.MEMBER);

        // When & Then
        assertThatThrownBy(() -> tokenValidator.validateAccessToken(expiredToken))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_JWT_TOKEN);
    }

    @Test
    @DisplayName("블랙리스트에 있는 Access Token 검증 시 예외")
    void validateAccessToken_blacklistedToken_throwsException() {
        // Given
        String validToken = AuthTestFixture.createValidAccessToken(1L, "test@example.com", UserRole.NORMAL, UserType.MEMBER);
        String tokenId = jwtTokenProvider.getTokenId(validToken);
        
        given(tokenBlacklistStorage.isTokenBlacklisted(tokenId)).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> tokenValidator.validateAccessToken(validToken))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_JWT_TOKEN);
    }

    // ==================== Refresh Token 검증 테스트 ====================

    @Test
    @DisplayName("유효한 Refresh Token 검증 성공")
    void validateRefreshToken_validToken_success() {
        // Given
        String validToken = AuthTestFixture.createValidRefreshToken(1L, UserType.MEMBER);
        String tokenId = jwtTokenProvider.getTokenId(validToken);
        
        given(tokenBlacklistStorage.isTokenBlacklisted(tokenId)).willReturn(false);

        // When
        TokenValidator.TokenValidationResult result = tokenValidator.validateRefreshToken(validToken);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.userType()).isEqualTo(UserType.MEMBER.getValue());
        assertThat(result.tokenId()).isEqualTo(tokenId);
    }

    @Test
    @DisplayName("판매자 Refresh Token 검증 성공")
    void validateRefreshToken_sellerToken_success() {
        // Given
        String sellerToken = AuthTestFixture.createValidRefreshToken(1L, UserType.SELLER);
        String tokenId = jwtTokenProvider.getTokenId(sellerToken);
        
        given(tokenBlacklistStorage.isTokenBlacklisted(tokenId)).willReturn(false);

        // When
        TokenValidator.TokenValidationResult result = tokenValidator.validateRefreshToken(sellerToken);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.userType()).isEqualTo(UserType.SELLER.getValue());
        assertThat(result.tokenId()).isEqualTo(tokenId);
    }

    @Test
    @DisplayName("null Refresh Token 검증 시 예외")
    void validateRefreshToken_nullToken_throwsException() {
        // Given
        String nullToken = AuthTestFixture.createNullToken();

        // When & Then
        assertThatThrownBy(() -> tokenValidator.validateRefreshToken(nullToken))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MISSING_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("빈 Refresh Token 검증 시 예외")
    void validateRefreshToken_emptyToken_throwsException() {
        // Given
        String emptyToken = AuthTestFixture.createEmptyToken();

        // When & Then
        assertThatThrownBy(() -> tokenValidator.validateRefreshToken(emptyToken))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MISSING_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("잘못된 형식의 Refresh Token 검증 시 예외")
    void validateRefreshToken_invalidToken_throwsException() {
        // Given
        String invalidToken = AuthTestFixture.createInvalidToken();

        // When & Then
        assertThatThrownBy(() -> tokenValidator.validateRefreshToken(invalidToken))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_JWT_TOKEN);
    }

    @Test
    @DisplayName("Access Token을 Refresh Token으로 검증 시 예외")
    void validateRefreshToken_accessToken_throwsException() {
        // Given
        String accessToken = AuthTestFixture.createValidAccessToken(1L, "test@example.com", UserRole.NORMAL, UserType.MEMBER);

        // When & Then
        assertThatThrownBy(() -> tokenValidator.validateRefreshToken(accessToken))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("만료된 Refresh Token 검증 시 예외")
    void validateRefreshToken_expiredToken_throwsException() {
        // Given
        String expiredToken = AuthTestFixture.createExpiredAccessToken(1L, "test@example.com", UserRole.NORMAL, UserType.MEMBER);

        // When & Then
        assertThatThrownBy(() -> tokenValidator.validateRefreshToken(expiredToken))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_JWT_TOKEN);
    }

    @Test
    @DisplayName("블랙리스트에 있는 Refresh Token 검증 시 예외")
    void validateRefreshToken_blacklistedToken_throwsException() {
        // Given
        String validToken = AuthTestFixture.createValidRefreshToken(1L, UserType.MEMBER);
        String tokenId = jwtTokenProvider.getTokenId(validToken);
        
        given(tokenBlacklistStorage.isTokenBlacklisted(tokenId)).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> tokenValidator.validateRefreshToken(validToken))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REFRESH_TOKEN);
    }

    // ==================== 경계 조건 테스트 ====================

    @Test
    @DisplayName("공백 Access Token 검증 시 예외")
    void validateAccessToken_blankToken_throwsException() {
        // Given
        String blankToken = "   ";

        // When & Then
        assertThatThrownBy(() -> tokenValidator.validateAccessToken(blankToken))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MISSING_AUTHORIZATION_HEADER);
    }

    @Test
    @DisplayName("공백 Refresh Token 검증 시 예외")
    void validateRefreshToken_blankToken_throwsException() {
        // Given
        String blankToken = "   ";

        // When & Then
        assertThatThrownBy(() -> tokenValidator.validateRefreshToken(blankToken))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MISSING_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("다른 사용자의 Access Token 검증 성공")
    void validateAccessToken_differentUser_success() {
        // Given
        String validToken = AuthTestFixture.createValidAccessToken(999L, "other@example.com", UserRole.ADMIN, UserType.MEMBER);
        String tokenId = jwtTokenProvider.getTokenId(validToken);
        
        given(tokenBlacklistStorage.isTokenBlacklisted(tokenId)).willReturn(false);

        // When
        TokenValidator.TokenValidationResult result = tokenValidator.validateAccessToken(validToken);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(999L);
        assertThat(result.userType()).isEqualTo(UserType.MEMBER.getValue());
        assertThat(result.tokenId()).isEqualTo(tokenId);
    }

    @Test
    @DisplayName("다른 사용자의 Refresh Token 검증 성공")
    void validateRefreshToken_differentUser_success() {
        // Given
        String validToken = AuthTestFixture.createValidRefreshToken(999L, UserType.SELLER);
        String tokenId = jwtTokenProvider.getTokenId(validToken);
        
        given(tokenBlacklistStorage.isTokenBlacklisted(tokenId)).willReturn(false);

        // When
        TokenValidator.TokenValidationResult result = tokenValidator.validateRefreshToken(validToken);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(999L);
        assertThat(result.userType()).isEqualTo(UserType.SELLER.getValue());
        assertThat(result.tokenId()).isEqualTo(tokenId);
    }

    // ==================== TokenValidationResult 테스트 ====================

    @Test
    @DisplayName("TokenValidationResult 생성 성공")
    void tokenValidationResult_creation_success() {
        // Given
        Long userId = 1L;
        String userType = UserType.MEMBER.getValue();
        String tokenId = "test-token-id";

        // When
        TokenValidator.TokenValidationResult result = TokenValidator.TokenValidationResult.of(userId, userType, tokenId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.userType()).isEqualTo(userType);
        assertThat(result.tokenId()).isEqualTo(tokenId);
    }

    @Test
    @DisplayName("TokenValidationResult 불변성 확인")
    void tokenValidationResult_immutability() {
        // Given
        Long userId = 1L;
        String userType = UserType.MEMBER.getValue();
        String tokenId = "test-token-id";

        // When
        TokenValidator.TokenValidationResult result = TokenValidator.TokenValidationResult.of(userId, userType, tokenId);

        // Then
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.userType()).isEqualTo(userType);
        assertThat(result.tokenId()).isEqualTo(tokenId);
        
        // 불변 객체이므로 필드 변경 불가능
        // result.userId() = 2L; // 컴파일 에러
    }

    // ==================== 통합 테스트 ====================

    @Test
    @DisplayName("Access Token과 Refresh Token 동시 검증 성공")
    void validateBothTokens_success() {
        // Given
        String accessToken = AuthTestFixture.createValidAccessToken(1L, "test@example.com", UserRole.NORMAL, UserType.MEMBER);
        String refreshToken = AuthTestFixture.createValidRefreshToken(1L, UserType.MEMBER);
        
        String accessTokenId = jwtTokenProvider.getTokenId(accessToken);
        String refreshTokenId = jwtTokenProvider.getTokenId(refreshToken);
        
        given(tokenBlacklistStorage.isTokenBlacklisted(accessTokenId)).willReturn(false);
        given(tokenBlacklistStorage.isTokenBlacklisted(refreshTokenId)).willReturn(false);

        // When
        TokenValidator.TokenValidationResult accessResult = tokenValidator.validateAccessToken(accessToken);
        TokenValidator.TokenValidationResult refreshResult = tokenValidator.validateRefreshToken(refreshToken);

        // Then
        assertThat(accessResult.userId()).isEqualTo(1L);
        assertThat(accessResult.userType()).isEqualTo(UserType.MEMBER.getValue());
        assertThat(accessResult.tokenId()).isEqualTo(accessTokenId);
        
        assertThat(refreshResult.userId()).isEqualTo(1L);
        assertThat(refreshResult.userType()).isEqualTo(UserType.MEMBER.getValue());
        assertThat(refreshResult.tokenId()).isEqualTo(refreshTokenId);
    }

    @Test
    @DisplayName("판매자 Access Token과 Refresh Token 동시 검증 성공")
    void validateSellerTokens_success() {
        // Given
        String accessToken = AuthTestFixture.createValidAccessToken(1L, "seller@example.com", UserRole.SELLER, UserType.SELLER);
        String refreshToken = AuthTestFixture.createValidRefreshToken(1L, UserType.SELLER);
        
        String accessTokenId = jwtTokenProvider.getTokenId(accessToken);
        String refreshTokenId = jwtTokenProvider.getTokenId(refreshToken);
        
        given(tokenBlacklistStorage.isTokenBlacklisted(accessTokenId)).willReturn(false);
        given(tokenBlacklistStorage.isTokenBlacklisted(refreshTokenId)).willReturn(false);

        // When
        TokenValidator.TokenValidationResult accessResult = tokenValidator.validateAccessToken(accessToken);
        TokenValidator.TokenValidationResult refreshResult = tokenValidator.validateRefreshToken(refreshToken);

        // Then
        assertThat(accessResult.userId()).isEqualTo(1L);
        assertThat(accessResult.userType()).isEqualTo(UserType.SELLER.getValue());
        assertThat(accessResult.tokenId()).isEqualTo(accessTokenId);
        
        assertThat(refreshResult.userId()).isEqualTo(1L);
        assertThat(refreshResult.userType()).isEqualTo(UserType.SELLER.getValue());
        assertThat(refreshResult.tokenId()).isEqualTo(refreshTokenId);
    }
} 