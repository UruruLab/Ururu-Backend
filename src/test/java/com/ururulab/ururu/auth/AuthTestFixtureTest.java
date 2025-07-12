package com.ururulab.ururu.auth;

import com.ururulab.ururu.auth.constants.UserRole;
import com.ururulab.ururu.auth.constants.UserType;
import com.ururulab.ururu.auth.dto.info.SocialMemberInfo;
import com.ururulab.ururu.auth.dto.request.SellerLoginRequest;
import com.ururulab.ururu.auth.dto.response.SocialLoginResponse;
import com.ururulab.ururu.auth.jwt.JwtProperties;
import com.ururulab.ururu.auth.jwt.JwtTokenProvider;
import com.ururulab.ururu.auth.service.TokenValidator;
import com.ururulab.ururu.auth.service.UserInfoService;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.entity.enumerated.SocialProvider;
import com.ururulab.ururu.seller.domain.entity.Seller;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AuthTestFixture의 동작을 검증하는 테스트.
 * 
 * 테스트 데이터 생성 메서드들이 올바르게 동작하는지 확인합니다.
 */
@DisplayName("AuthTestFixture 테스트")
class AuthTestFixtureTest {

    @Test
    @DisplayName("JWT 토큰 생성 테스트")
    void createJwtTokens() {
        // Given
        Long userId = 1L;
        String email = "test@example.com";
        UserRole role = UserRole.NORMAL;
        UserType userType = UserType.MEMBER;

        // When
        String accessToken = AuthTestFixture.createValidAccessToken(userId, email, role, userType);
        String refreshToken = AuthTestFixture.createValidRefreshToken(userId, userType);

        // Then
        assertThat(accessToken).isNotNull().isNotEmpty();
        assertThat(refreshToken).isNotNull().isNotEmpty();
        
        // JWT Token Provider로 검증
        JwtTokenProvider provider = AuthTestFixture.createTestJwtTokenProvider();
        assertThat(provider.validateToken(accessToken)).isTrue();
        assertThat(provider.validateToken(refreshToken)).isTrue();
        assertThat(provider.getMemberId(accessToken)).isEqualTo(userId);
        assertThat(provider.getEmail(accessToken)).isEqualTo(email);
        assertThat(provider.getRole(accessToken)).isEqualTo(role.getValue());
        assertThat(provider.getUserType(accessToken)).isEqualTo(userType.getValue());
    }

    @Test
    @DisplayName("만료된 토큰 생성 테스트")
    void createExpiredToken() {
        // Given
        Long userId = 1L;
        String email = "test@example.com";
        UserRole role = UserRole.NORMAL;
        UserType userType = UserType.MEMBER;

        // When
        String expiredToken = AuthTestFixture.createExpiredAccessToken(userId, email, role, userType);

        // Then
        assertThat(expiredToken).isNotNull().isNotEmpty();
        
        JwtTokenProvider provider = AuthTestFixture.createTestJwtTokenProvider();
        assertThat(provider.isTokenExpired(expiredToken)).isTrue();
    }

    @Test
    @DisplayName("소셜 회원 정보 생성 테스트")
    void createSocialMemberInfo() {
        // When
        SocialMemberInfo googleInfo = AuthTestFixture.createGoogleSocialMemberInfo();
        SocialMemberInfo kakaoInfo = AuthTestFixture.createKakaoSocialMemberInfo();

        // Then
        assertThat(googleInfo.socialId()).isEqualTo("google123456789");
        assertThat(googleInfo.email()).isEqualTo("test@example.com");
        assertThat(googleInfo.nickname()).isEqualTo("테스트유저");
        assertThat(googleInfo.provider()).isEqualTo(SocialProvider.GOOGLE);

        assertThat(kakaoInfo.socialId()).isEqualTo("kakao987654321");
        assertThat(kakaoInfo.email()).isEqualTo("test@kakao.com");
        assertThat(kakaoInfo.nickname()).isEqualTo("카카오유저");
        assertThat(kakaoInfo.provider()).isEqualTo(SocialProvider.KAKAO);
    }

    @Test
    @DisplayName("이메일이 없는 소셜 회원 정보 생성 테스트")
    void createSocialMemberInfoWithoutEmail() {
        // When
        SocialMemberInfo info = AuthTestFixture.createSocialMemberInfoWithoutEmail();

        // Then
        assertThat(info.socialId()).isEqualTo("noemail123");
        assertThat(info.email()).isNull();
        assertThat(info.nickname()).isEqualTo("이메일없는유저");
        assertThat(info.provider()).isEqualTo(SocialProvider.GOOGLE);
    }

    @Test
    @DisplayName("Member 생성 테스트")
    void createMember() {
        // Given
        Long memberId = 1L;
        String nickname = "테스트유저";
        String email = "test@example.com";

        // When
        Member member = AuthTestFixture.createMember(memberId, nickname, email);

        // Then
        assertThat(member.getId()).isEqualTo(memberId);
        assertThat(member.getNickname()).isEqualTo(nickname);
        assertThat(member.getEmail()).isEqualTo(email);
        assertThat(member.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("Seller 생성 테스트")
    void createSeller() {
        // Given
        Long sellerId = 1L;
        String email = "seller@example.com";
        String name = "테스트브랜드";

        // When
        Seller seller = AuthTestFixture.createSeller(sellerId, email, name);

        // Then
        assertThat(seller.getId()).isEqualTo(sellerId);
        assertThat(seller.getEmail()).isEqualTo(email);
        assertThat(seller.getName()).isEqualTo(name);
        assertThat(seller.getIsDeleted()).isFalse();
    }

    @Test
    @DisplayName("삭제된 Member 생성 테스트")
    void createDeletedMember() {
        // Given
        Long memberId = 1L;
        String nickname = "삭제된유저";
        String email = "deleted@example.com";

        // When
        Member member = AuthTestFixture.createDeletedMember(memberId, nickname, email);

        // Then
        assertThat(member.getId()).isEqualTo(memberId);
        assertThat(member.getNickname()).isEqualTo(nickname);
        assertThat(member.getEmail()).isEqualTo(email);
        assertThat(member.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("판매자 로그인 요청 생성 테스트")
    void createSellerLoginRequest() {
        // Given
        String email = "seller@example.com";
        String password = "password123";

        // When
        SellerLoginRequest request = AuthTestFixture.createSellerLoginRequest(email, password);

        // Then
        assertThat(request.email()).isEqualTo(email);
        assertThat(request.password()).isEqualTo(password);
    }

    @Test
    @DisplayName("유효한 판매자 로그인 요청 생성 테스트")
    void createValidSellerLoginRequest() {
        // When
        SellerLoginRequest request = AuthTestFixture.createValidSellerLoginRequest();

        // Then
        assertThat(request.email()).isEqualTo("seller@example.com");
        assertThat(request.password()).isEqualTo("password123");
    }

    @Test
    @DisplayName("토큰 검증 결과 생성 테스트")
    void createTokenValidationResult() {
        // Given
        Long userId = 1L;
        String userType = UserType.MEMBER.getValue();
        String tokenId = "test-token-id";

        // When
        TokenValidator.TokenValidationResult result = AuthTestFixture.createTokenValidationResult(userId, userType, tokenId);

        // Then
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.userType()).isEqualTo(userType);
        assertThat(result.tokenId()).isEqualTo(tokenId);
    }

    @Test
    @DisplayName("소셜 로그인 응답 생성 테스트")
    void createSocialLoginResponse() {
        // Given
        String accessToken = AuthTestFixture.createValidAccessToken(1L, "test@example.com", UserRole.NORMAL, UserType.MEMBER);
        String refreshToken = AuthTestFixture.createValidRefreshToken(1L, UserType.MEMBER);
        Long expiresIn = 3600L;
        SocialLoginResponse.MemberInfo memberInfo = SocialLoginResponse.MemberInfo.of(
                1L, "test@example.com", "테스트유저", null, UserType.MEMBER.getValue()
        );

        // When
        SocialLoginResponse response = AuthTestFixture.createSocialLoginResponse(accessToken, refreshToken, expiresIn, memberInfo);

        // Then
        assertThat(response.accessToken()).isEqualTo(accessToken);
        assertThat(response.refreshToken()).isEqualTo(refreshToken);
        assertThat(response.expiresIn()).isEqualTo(expiresIn);
        assertThat(response.memberInfo()).isEqualTo(memberInfo);
    }

    @Test
    @DisplayName("유효한 소셜 로그인 응답 생성 테스트")
    void createValidSocialLoginResponse() {
        // When
        SocialLoginResponse response = AuthTestFixture.createValidSocialLoginResponse();

        // Then
        assertThat(response.accessToken()).isNotNull().isNotEmpty();
        assertThat(response.refreshToken()).isNotNull().isNotEmpty();
        assertThat(response.expiresIn()).isEqualTo(3600L);
        assertThat(response.memberInfo().memberId()).isEqualTo(1L);
        assertThat(response.memberInfo().email()).isEqualTo("test@example.com");
        assertThat(response.memberInfo().nickname()).isEqualTo("테스트유저");
        assertThat(response.memberInfo().userType()).isEqualTo(UserType.MEMBER.getValue());
    }

    @Test
    @DisplayName("UserInfo 생성 테스트")
    void createUserInfo() {
        // Given
        String email = "test@example.com";
        String role = "NORMAL";

        // When
        UserInfoService.UserInfo userInfo = AuthTestFixture.createUserInfo(email, role);

        // Then
        assertThat(userInfo.email()).isEqualTo(email);
        assertThat(userInfo.role()).isEqualTo(role);
    }

    @Test
    @DisplayName("예외 상황 테스트 데이터 생성 테스트")
    void createExceptionTestData() {
        // When & Then
        assertThat(AuthTestFixture.createNonExistentUserId()).isEqualTo(999999L);
        assertThat(AuthTestFixture.createInvalidUserType()).isEqualTo("INVALID_TYPE");
        assertThat(AuthTestFixture.createInvalidUserRole()).isEqualTo("INVALID_ROLE");
        assertThat(AuthTestFixture.createInvalidToken()).isEqualTo("invalid.token.format");
        assertThat(AuthTestFixture.createNullToken()).isNull();
        assertThat(AuthTestFixture.createEmptyToken()).isEmpty();
    }

    @Test
    @DisplayName("잘못된 소셜 회원 정보 생성 시 예외 발생 테스트")
    void createInvalidSocialMemberInfo() {
        // When & Then
        assertThatThrownBy(() -> AuthTestFixture.createInvalidSocialMemberInfo())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("소셜 ID는 필수입니다");
    }
} 