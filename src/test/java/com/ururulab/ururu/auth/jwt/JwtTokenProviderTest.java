package com.ururulab.ururu.auth.jwt;

import com.ururulab.ururu.auth.AuthTestFixture;
import com.ururulab.ururu.auth.constants.UserRole;
import com.ururulab.ururu.auth.constants.UserType;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JwtTokenProvider 테스트.
 * 
 * JWT 토큰 생성, 검증, 파싱 기능을 포괄적으로 테스트합니다.
 */
@DisplayName("JwtTokenProvider 테스트")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        jwtProperties = AuthTestFixture.createTestJwtProperties();
        jwtTokenProvider = AuthTestFixture.createTestJwtTokenProvider();
    }

    // ==================== 토큰 생성 테스트 ====================

    @Test
    @DisplayName("유효한 Access Token 생성 성공")
    void generateAccessToken_success() {
        // Given
        Long userId = 1L;
        String email = "test@example.com";
        UserRole role = UserRole.NORMAL;
        UserType userType = UserType.MEMBER;

        // When
        String accessToken = jwtTokenProvider.generateAccessToken(userId, email, role, userType);

        // Then
        assertThat(accessToken).isNotNull().isNotEmpty();
        assertThat(jwtTokenProvider.validateToken(accessToken)).isTrue();
        assertThat(jwtTokenProvider.getMemberId(accessToken)).isEqualTo(userId);
        assertThat(jwtTokenProvider.getEmail(accessToken)).isEqualTo(email);
        assertThat(jwtTokenProvider.getRole(accessToken)).isEqualTo(role.getValue());
        assertThat(jwtTokenProvider.getUserType(accessToken)).isEqualTo(userType.getValue());
        assertThat(jwtTokenProvider.isAccessToken(accessToken)).isTrue();
        assertThat(jwtTokenProvider.isRefreshToken(accessToken)).isFalse();
    }

    @Test
    @DisplayName("유효한 Refresh Token 생성 성공")
    void generateRefreshToken_success() {
        // Given
        Long userId = 1L;
        UserType userType = UserType.MEMBER;

        // When
        String refreshToken = jwtTokenProvider.generateRefreshToken(userId, userType);

        // Then
        assertThat(refreshToken).isNotNull().isNotEmpty();
        assertThat(jwtTokenProvider.validateToken(refreshToken)).isTrue();
        assertThat(jwtTokenProvider.getMemberId(refreshToken)).isEqualTo(userId);
        assertThat(jwtTokenProvider.getUserType(refreshToken)).isEqualTo(userType.getValue());
        assertThat(jwtTokenProvider.isRefreshToken(refreshToken)).isTrue();
        assertThat(jwtTokenProvider.isAccessToken(refreshToken)).isFalse();
        // Refresh Token은 email과 role이 없어야 함
        assertThat(jwtTokenProvider.getEmail(refreshToken)).isNull();
        assertThat(jwtTokenProvider.getRole(refreshToken)).isNull();
    }

    @Test
    @DisplayName("판매자 Access Token 생성 성공")
    void generateSellerAccessToken_success() {
        // Given
        Long userId = 1L;
        String email = "seller@example.com";
        UserRole role = UserRole.SELLER;
        UserType userType = UserType.SELLER;

        // When
        String accessToken = jwtTokenProvider.generateAccessToken(userId, email, role, userType);

        // Then
        assertThat(accessToken).isNotNull().isNotEmpty();
        assertThat(jwtTokenProvider.getUserType(accessToken)).isEqualTo(UserType.SELLER.getValue());
        assertThat(jwtTokenProvider.getRole(accessToken)).isEqualTo(UserRole.SELLER.getValue());
    }

    // ==================== 토큰 생성 예외 테스트 ====================

    @Test
    @DisplayName("Access Token 생성 시 null userId 예외")
    void generateAccessToken_nullUserId_throwsException() {
        // Given
        String email = "test@example.com";
        UserRole role = UserRole.NORMAL;
        UserType userType = UserType.MEMBER;

        // When & Then
        assertThatThrownBy(() -> jwtTokenProvider.generateAccessToken(null, email, role, userType))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User ID cannot be null");
    }

    @Test
    @DisplayName("Access Token 생성 시 null email 예외")
    void generateAccessToken_nullEmail_throwsException() {
        // Given
        Long userId = 1L;
        UserRole role = UserRole.NORMAL;
        UserType userType = UserType.MEMBER;

        // When & Then
        assertThatThrownBy(() -> jwtTokenProvider.generateAccessToken(userId, null, role, userType))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email cannot be null or blank");
    }

    @Test
    @DisplayName("Access Token 생성 시 빈 email 예외")
    void generateAccessToken_emptyEmail_throwsException() {
        // Given
        Long userId = 1L;
        UserRole role = UserRole.NORMAL;
        UserType userType = UserType.MEMBER;

        // When & Then
        assertThatThrownBy(() -> jwtTokenProvider.generateAccessToken(userId, "", role, userType))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email cannot be null or blank");
    }

    @Test
    @DisplayName("Access Token 생성 시 null role 예외")
    void generateAccessToken_nullRole_throwsException() {
        // Given
        Long userId = 1L;
        String email = "test@example.com";
        UserType userType = UserType.MEMBER;

        // When & Then
        assertThatThrownBy(() -> jwtTokenProvider.generateAccessToken(userId, email, null, userType))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Role cannot be null");
    }

    @Test
    @DisplayName("Access Token 생성 시 null userType 예외")
    void generateAccessToken_nullUserType_throwsException() {
        // Given
        Long userId = 1L;
        String email = "test@example.com";
        UserRole role = UserRole.NORMAL;

        // When & Then
        assertThatThrownBy(() -> jwtTokenProvider.generateAccessToken(userId, email, role, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User type cannot be null");
    }

    @Test
    @DisplayName("Refresh Token 생성 시 null userId 예외")
    void generateRefreshToken_nullUserId_throwsException() {
        // Given
        UserType userType = UserType.MEMBER;

        // When & Then
        assertThatThrownBy(() -> jwtTokenProvider.generateRefreshToken(null, userType))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User ID cannot be null");
    }

    @Test
    @DisplayName("Refresh Token 생성 시 null userType 예외")
    void generateRefreshToken_nullUserType_throwsException() {
        // Given
        Long userId = 1L;

        // When & Then
        assertThatThrownBy(() -> jwtTokenProvider.generateRefreshToken(userId, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User type cannot be null");
    }

    // ==================== 토큰 검증 테스트 ====================

    @Test
    @DisplayName("유효한 토큰 검증 성공")
    void validateToken_validToken_returnsTrue() {
        // Given
        String validToken = AuthTestFixture.createValidAccessToken(1L, "test@example.com", UserRole.NORMAL, UserType.MEMBER);

        // When
        boolean isValid = jwtTokenProvider.validateToken(validToken);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("잘못된 형식의 토큰 검증 실패")
    void validateToken_invalidToken_returnsFalse() {
        // Given
        String invalidToken = AuthTestFixture.createInvalidToken();

        // When
        boolean isValid = jwtTokenProvider.validateToken(invalidToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("null 토큰 검증 실패")
    void validateToken_nullToken_returnsFalse() {
        // Given
        String nullToken = AuthTestFixture.createNullToken();

        // When
        boolean isValid = jwtTokenProvider.validateToken(nullToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("빈 토큰 검증 실패")
    void validateToken_emptyToken_returnsFalse() {
        // Given
        String emptyToken = AuthTestFixture.createEmptyToken();

        // When
        boolean isValid = jwtTokenProvider.validateToken(emptyToken);

        // Then
        assertThat(isValid).isFalse();
    }

    // ==================== 토큰 만료 테스트 ====================

    @Test
    @DisplayName("유효한 토큰 만료 확인")
    void isTokenExpired_validToken_returnsFalse() {
        // Given
        String validToken = AuthTestFixture.createValidAccessToken(1L, "test@example.com", UserRole.NORMAL, UserType.MEMBER);

        // When
        boolean isExpired = jwtTokenProvider.isTokenExpired(validToken);

        // Then
        assertThat(isExpired).isFalse();
    }

    @Test
    @DisplayName("만료된 토큰 확인")
    void isTokenExpired_expiredToken_returnsTrue() {
        // Given
        String expiredToken = AuthTestFixture.createExpiredAccessToken(1L, "test@example.com", UserRole.NORMAL, UserType.MEMBER);

        // When
        boolean isExpired = jwtTokenProvider.isTokenExpired(expiredToken);

        // Then
        assertThat(isExpired).isTrue();
    }

    @Test
    @DisplayName("잘못된 형식의 토큰 만료 확인 시 예외")
    void isTokenExpired_invalidToken_throwsException() {
        // Given
        String invalidToken = AuthTestFixture.createInvalidToken();

        // When & Then
        assertThatThrownBy(() -> jwtTokenProvider.isTokenExpired(invalidToken))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_JWT_TOKEN);
    }

    // ==================== 토큰 정보 추출 테스트 ====================

    @Test
    @DisplayName("토큰에서 사용자 ID 추출 성공")
    void getMemberId_success() {
        // Given
        Long expectedUserId = 1L;
        String token = AuthTestFixture.createValidAccessToken(expectedUserId, "test@example.com", UserRole.NORMAL, UserType.MEMBER);

        // When
        Long userId = jwtTokenProvider.getMemberId(token);

        // Then
        assertThat(userId).isEqualTo(expectedUserId);
    }

    @Test
    @DisplayName("토큰에서 이메일 추출 성공")
    void getEmail_success() {
        // Given
        String expectedEmail = "test@example.com";
        String token = AuthTestFixture.createValidAccessToken(1L, expectedEmail, UserRole.NORMAL, UserType.MEMBER);

        // When
        String email = jwtTokenProvider.getEmail(token);

        // Then
        assertThat(email).isEqualTo(expectedEmail);
    }

    @Test
    @DisplayName("토큰에서 역할 추출 성공")
    void getRole_success() {
        // Given
        UserRole expectedRole = UserRole.SELLER;
        String token = AuthTestFixture.createValidAccessToken(1L, "test@example.com", expectedRole, UserType.SELLER);

        // When
        String role = jwtTokenProvider.getRole(token);

        // Then
        assertThat(role).isEqualTo(expectedRole.getValue());
    }

    @Test
    @DisplayName("토큰에서 사용자 타입 추출 성공")
    void getUserType_success() {
        // Given
        UserType expectedUserType = UserType.SELLER;
        String token = AuthTestFixture.createValidAccessToken(1L, "test@example.com", UserRole.SELLER, expectedUserType);

        // When
        String userType = jwtTokenProvider.getUserType(token);

        // Then
        assertThat(userType).isEqualTo(expectedUserType.getValue());
    }

    @Test
    @DisplayName("토큰에서 사용자 타입 Enum 추출 성공")
    void getUserTypeAsEnum_success() {
        // Given
        UserType expectedUserType = UserType.MEMBER;
        String token = AuthTestFixture.createValidAccessToken(1L, "test@example.com", UserRole.NORMAL, expectedUserType);

        // When
        UserType userType = jwtTokenProvider.getUserTypeAsEnum(token);

        // Then
        assertThat(userType).isEqualTo(expectedUserType);
    }

    @Test
    @DisplayName("토큰에서 역할 Enum 추출 성공")
    void getRoleAsEnum_success() {
        // Given
        UserRole expectedRole = UserRole.ADMIN;
        String token = AuthTestFixture.createValidAccessToken(1L, "test@example.com", expectedRole, UserType.MEMBER);

        // When
        UserRole role = jwtTokenProvider.getRoleAsEnum(token);

        // Then
        assertThat(role).isEqualTo(expectedRole);
    }

    @Test
    @DisplayName("토큰에서 토큰 ID 추출 성공")
    void getTokenId_success() {
        // Given
        String token = AuthTestFixture.createValidAccessToken(1L, "test@example.com", UserRole.NORMAL, UserType.MEMBER);

        // When
        String tokenId = jwtTokenProvider.getTokenId(token);

        // Then
        assertThat(tokenId).isNotNull().isNotEmpty();
    }

    // ==================== 토큰 타입 확인 테스트 ====================

    @Test
    @DisplayName("Access Token 타입 확인 성공")
    void isAccessToken_success() {
        // Given
        String accessToken = AuthTestFixture.createValidAccessToken(1L, "test@example.com", UserRole.NORMAL, UserType.MEMBER);

        // When
        boolean isAccessToken = jwtTokenProvider.isAccessToken(accessToken);

        // Then
        assertThat(isAccessToken).isTrue();
    }

    @Test
    @DisplayName("Refresh Token 타입 확인 성공")
    void isRefreshToken_success() {
        // Given
        String refreshToken = AuthTestFixture.createValidRefreshToken(1L, UserType.MEMBER);

        // When
        boolean isRefreshToken = jwtTokenProvider.isRefreshToken(refreshToken);

        // Then
        assertThat(isRefreshToken).isTrue();
    }

    @Test
    @DisplayName("Access Token이 Refresh Token이 아님 확인")
    void isRefreshToken_accessToken_returnsFalse() {
        // Given
        String accessToken = AuthTestFixture.createValidAccessToken(1L, "test@example.com", UserRole.NORMAL, UserType.MEMBER);

        // When
        boolean isRefreshToken = jwtTokenProvider.isRefreshToken(accessToken);

        // Then
        assertThat(isRefreshToken).isFalse();
    }

    @Test
    @DisplayName("Refresh Token이 Access Token이 아님 확인")
    void isAccessToken_refreshToken_returnsFalse() {
        // Given
        String refreshToken = AuthTestFixture.createValidRefreshToken(1L, UserType.MEMBER);

        // When
        boolean isAccessToken = jwtTokenProvider.isAccessToken(refreshToken);

        // Then
        assertThat(isAccessToken).isFalse();
    }

    // ==================== 토큰 만료 시간 테스트 ====================

    @Test
    @DisplayName("토큰 남은 만료 시간 확인")
    void getRemainingExpiry_success() {
        // Given
        String token = AuthTestFixture.createValidAccessToken(1L, "test@example.com", UserRole.NORMAL, UserType.MEMBER);

        // When
        long remainingExpiry = jwtTokenProvider.getRemainingExpiry(token);

        // Then
        assertThat(remainingExpiry).isGreaterThan(0);
        assertThat(remainingExpiry).isLessThanOrEqualTo(jwtProperties.getAccessTokenExpiry());
    }

    @Test
    @DisplayName("만료된 토큰의 남은 시간은 0")
    void getRemainingExpiry_expiredToken_returnsZero() {
        // Given
        String expiredToken = AuthTestFixture.createExpiredAccessToken(1L, "test@example.com", UserRole.NORMAL, UserType.MEMBER);

        // When & Then
        assertThatThrownBy(() -> jwtTokenProvider.getRemainingExpiry(expiredToken))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    // ==================== 설정 정보 테스트 ====================

    @Test
    @DisplayName("Access Token 만료 시간 조회")
    void getAccessTokenExpiry_success() {
        // When
        Long expiry = jwtTokenProvider.getAccessTokenExpiry();

        // Then
        assertThat(expiry).isEqualTo(jwtProperties.getAccessTokenExpiry());
    }

    @Test
    @DisplayName("Refresh Token 만료 시간 조회")
    void getRefreshTokenExpirySeconds_success() {
        // When
        long expiry = jwtTokenProvider.getRefreshTokenExpirySeconds();

        // Then
        assertThat(expiry).isEqualTo(jwtProperties.getRefreshTokenExpiry());
    }

    // ==================== 예외 상황 테스트 ====================

    @Test
    @DisplayName("잘못된 형식의 토큰에서 사용자 ID 추출 시 예외")
    void getMemberId_invalidToken_throwsException() {
        // Given
        String invalidToken = AuthTestFixture.createInvalidToken();

        // When & Then
        assertThatThrownBy(() -> jwtTokenProvider.getMemberId(invalidToken))
                .isInstanceOf(io.jsonwebtoken.JwtException.class);
    }

    @Test
    @DisplayName("잘못된 형식의 토큰에서 이메일 추출 시 예외")
    void getEmail_invalidToken_throwsException() {
        // Given
        String invalidToken = AuthTestFixture.createInvalidToken();

        // When & Then
        assertThatThrownBy(() -> jwtTokenProvider.getEmail(invalidToken))
                .isInstanceOf(io.jsonwebtoken.JwtException.class);
    }

    @Test
    @DisplayName("잘못된 형식의 토큰에서 역할 추출 시 예외")
    void getRole_invalidToken_throwsException() {
        // Given
        String invalidToken = AuthTestFixture.createInvalidToken();

        // When & Then
        assertThatThrownBy(() -> jwtTokenProvider.getRole(invalidToken))
                .isInstanceOf(io.jsonwebtoken.JwtException.class);
    }

    @Test
    @DisplayName("잘못된 형식의 토큰에서 사용자 타입 추출 시 예외")
    void getUserType_invalidToken_throwsException() {
        // Given
        String invalidToken = AuthTestFixture.createInvalidToken();

        // When & Then
        assertThatThrownBy(() -> jwtTokenProvider.getUserType(invalidToken))
                .isInstanceOf(io.jsonwebtoken.JwtException.class);
    }

    @Test
    @DisplayName("잘못된 형식의 토큰에서 토큰 ID 추출 시 예외")
    void getTokenId_invalidToken_throwsException() {
        // Given
        String invalidToken = AuthTestFixture.createInvalidToken();

        // When & Then
        assertThatThrownBy(() -> jwtTokenProvider.getTokenId(invalidToken))
                .isInstanceOf(io.jsonwebtoken.JwtException.class);
    }
} 