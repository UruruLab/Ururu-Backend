package com.ururulab.ururu.auth;

import com.ururulab.ururu.auth.constants.UserRole;
import com.ururulab.ururu.auth.constants.UserType;
import com.ururulab.ururu.auth.dto.info.SocialMemberInfo;
import com.ururulab.ururu.auth.dto.request.SellerLoginRequest;
import com.ururulab.ururu.auth.dto.response.SocialLoginResponse;
import com.ururulab.ururu.auth.jwt.JwtProperties;
import com.ururulab.ururu.auth.jwt.JwtTokenProvider;
import com.ururulab.ururu.auth.service.UserInfoService;
import com.ururulab.ururu.auth.service.TokenValidator;
import com.ururulab.ururu.global.domain.entity.enumerated.Gender;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.entity.enumerated.Role;
import com.ururulab.ururu.member.domain.entity.enumerated.SocialProvider;
import com.ururulab.ururu.seller.domain.entity.Seller;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Auth 관련 테스트를 위한 TestFixture 클래스.
 * 
 * JWT 토큰 생성, 소셜 로그인 데이터, 사용자 정보, 토큰 검증 데이터 등을 중앙화하여 관리합니다.
 */
public final class AuthTestFixture {

    private AuthTestFixture() {
        // 유틸리티 클래스이므로 인스턴스화 방지
    }

    // ==================== JWT 토큰 관련 ====================

    /**
     * 테스트용 JWT Properties 생성
     */
    public static JwtProperties createTestJwtProperties() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-secret-key-for-jwt-token-generation-in-test-environment-only");
        properties.setAccessTokenExpiry(3600L); // 1시간
        properties.setRefreshTokenExpiry(86400L); // 24시간
        properties.setIssuer("test-ururu");
        properties.setAudience("test-ururu-client");
        return properties;
    }

    /**
     * 테스트용 JWT Token Provider 생성
     */
    public static JwtTokenProvider createTestJwtTokenProvider() {
        return new JwtTokenProvider(createTestJwtProperties());
    }

    /**
     * 유효한 Access Token 생성
     */
    public static String createValidAccessToken(Long userId, String email, UserRole role, UserType userType) {
        JwtTokenProvider provider = createTestJwtTokenProvider();
        return provider.generateAccessToken(userId, email, role, userType);
    }

    /**
     * 유효한 Refresh Token 생성
     */
    public static String createValidRefreshToken(Long userId, UserType userType) {
        JwtTokenProvider provider = createTestJwtTokenProvider();
        return provider.generateRefreshToken(userId, userType);
    }

    /**
     * 만료된 Access Token 생성 (과거 시간으로 설정)
     */
    public static String createExpiredAccessToken(Long userId, String email, UserRole role, UserType userType) {
        // 만료된 토큰을 생성하기 위해 임시로 만료 시간을 0으로 설정
        JwtProperties expiredProperties = createTestJwtProperties();
        expiredProperties.setAccessTokenExpiry(-3600L); // 과거 시간
        JwtTokenProvider expiredProvider = new JwtTokenProvider(expiredProperties);
        return expiredProvider.generateAccessToken(userId, email, role, userType);
    }

    /**
     * 잘못된 형식의 토큰 생성
     */
    public static String createInvalidToken() {
        return "invalid.token.format";
    }

    /**
     * null 토큰
     */
    public static String createNullToken() {
        return null;
    }

    /**
     * 빈 토큰
     */
    public static String createEmptyToken() {
        return "";
    }

    // ==================== 소셜 로그인 관련 ====================

    /**
     * Google 소셜 회원 정보 생성
     */
    public static SocialMemberInfo createGoogleSocialMemberInfo() {
        return SocialMemberInfo.of(
                "google123456789",
                "test@example.com",
                "테스트유저",
                "https://example.com/profile.jpg",
                SocialProvider.GOOGLE
        );
    }

    /**
     * Kakao 소셜 회원 정보 생성
     */
    public static SocialMemberInfo createKakaoSocialMemberInfo() {
        return SocialMemberInfo.of(
                "kakao987654321",
                "test@kakao.com",
                "카카오유저",
                "https://kakao.com/profile.jpg",
                SocialProvider.KAKAO
        );
    }

    /**
     * 이메일이 없는 소셜 회원 정보 생성
     */
    public static SocialMemberInfo createSocialMemberInfoWithoutEmail() {
        return SocialMemberInfo.of(
                "noemail123",
                null,
                "이메일없는유저",
                null,
                SocialProvider.GOOGLE
        );
    }

    /**
     * 닉네임이 없는 소셜 회원 정보 생성
     */
    public static SocialMemberInfo createSocialMemberInfoWithoutNickname() {
        return SocialMemberInfo.of(
                "nonickname123",
                "test@example.com",
                null,
                null,
                SocialProvider.KAKAO
        );
    }

    /**
     * 잘못된 소셜 회원 정보 생성 (null socialId)
     */
    public static SocialMemberInfo createInvalidSocialMemberInfo() {
        return SocialMemberInfo.of(
                null,
                "test@example.com",
                "잘못된유저",
                null,
                SocialProvider.GOOGLE
        );
    }

    // ==================== 사용자 정보 관련 ====================

    /**
     * 테스트용 Member 생성
     */
    public static Member createMember(Long id, String nickname, String email) {
        Member member = Member.of(
                nickname,
                email,
                SocialProvider.GOOGLE,
                "social123",
                Gender.FEMALE,
                LocalDate.parse("1990-01-01"),
                "01012345678",
                null,
                Role.NORMAL
        );
        setMemberId(member, id);
        return member;
    }

    /**
     * 테스트용 Seller 생성
     */
    public static Seller createSeller(Long id, String email, String name) {
        Seller seller = Seller.of(
                name,
                "테스트사업자명",
                "테스트대표자",
                "1234567890",
                email,
                "encodedPassword123",
                "01012345678",
                "https://example.com/brand.jpg",
                "서울시 강남구",
                "테스트동 123-45",
                "2024-서울강남-1234"
        );
        setSellerId(seller, id);
        return seller;
    }

    /**
     * 삭제된 Member 생성
     */
    public static Member createDeletedMember(Long id, String nickname, String email) {
        Member member = createMember(id, nickname, email);
        setMemberDeleted(member, true);
        return member;
    }

    /**
     * 삭제된 Seller 생성
     */
    public static Seller createDeletedSeller(Long id, String email, String name) {
        Seller seller = createSeller(id, email, name);
        setSellerDeleted(seller, true);
        return seller;
    }

    /**
     * UserInfo 생성
     */
    public static UserInfoService.UserInfo createUserInfo(String email, String role) {
        return UserInfoService.UserInfo.of(email, role);
    }

    // ==================== 판매자 인증 관련 ====================

    /**
     * 판매자 로그인 요청 생성
     */
    public static SellerLoginRequest createSellerLoginRequest(String email, String password) {
        return SellerLoginRequest.of(email, password);
    }

    /**
     * 유효한 판매자 로그인 요청 생성
     */
    public static SellerLoginRequest createValidSellerLoginRequest() {
        return createSellerLoginRequest("seller@example.com", "password123");
    }

    /**
     * 잘못된 이메일 형식의 판매자 로그인 요청 생성
     */
    public static SellerLoginRequest createInvalidEmailSellerLoginRequest() {
        return createSellerLoginRequest("invalid-email", "password123");
    }

    /**
     * 빈 비밀번호의 판매자 로그인 요청 생성
     */
    public static SellerLoginRequest createEmptyPasswordSellerLoginRequest() {
        return createSellerLoginRequest("seller@example.com", "");
    }

    // ==================== 토큰 검증 관련 ====================

    /**
     * 토큰 검증 결과 생성
     */
    public static TokenValidator.TokenValidationResult createTokenValidationResult(Long userId, String userType, String tokenId) {
        return TokenValidator.TokenValidationResult.of(userId, userType, tokenId);
    }

    /**
     * 유효한 토큰 검증 결과 생성
     */
    public static TokenValidator.TokenValidationResult createValidTokenValidationResult() {
        return createTokenValidationResult(1L, UserType.MEMBER.getValue(), UUID.randomUUID().toString());
    }

    /**
     * 판매자 토큰 검증 결과 생성
     */
    public static TokenValidator.TokenValidationResult createSellerTokenValidationResult() {
        return createTokenValidationResult(1L, UserType.SELLER.getValue(), UUID.randomUUID().toString());
    }

    // ==================== 소셜 로그인 응답 관련 ====================

    /**
     * 소셜 로그인 응답 생성
     */
    public static SocialLoginResponse createSocialLoginResponse(String accessToken, String refreshToken, Long expiresIn, SocialLoginResponse.MemberInfo memberInfo) {
        return SocialLoginResponse.of(accessToken, refreshToken, expiresIn, memberInfo);
    }

    /**
     * 유효한 소셜 로그인 응답 생성
     */
    public static SocialLoginResponse createValidSocialLoginResponse() {
        String accessToken = createValidAccessToken(1L, "test@example.com", UserRole.NORMAL, UserType.MEMBER);
        String refreshToken = createValidRefreshToken(1L, UserType.MEMBER);
        SocialLoginResponse.MemberInfo memberInfo = SocialLoginResponse.MemberInfo.of(
                1L, "test@example.com", "테스트유저", null, UserType.MEMBER.getValue()
        );
        return createSocialLoginResponse(accessToken, refreshToken, 3600L, memberInfo);
    }

    /**
     * 판매자 소셜 로그인 응답 생성
     */
    public static SocialLoginResponse createSellerSocialLoginResponse() {
        String accessToken = createValidAccessToken(1L, "seller@example.com", UserRole.SELLER, UserType.SELLER);
        String refreshToken = createValidRefreshToken(1L, UserType.SELLER);
        SocialLoginResponse.MemberInfo memberInfo = SocialLoginResponse.MemberInfo.of(
                1L, "seller@example.com", "판매자", null, UserType.SELLER.getValue()
        );
        return createSocialLoginResponse(accessToken, refreshToken, 3600L, memberInfo);
    }

    // ==================== 예외 상황 테스트 데이터 ====================

    /**
     * 존재하지 않는 사용자 ID
     */
    public static Long createNonExistentUserId() {
        return 999999L;
    }

    /**
     * 잘못된 사용자 타입
     */
    public static String createInvalidUserType() {
        return "INVALID_TYPE";
    }

    /**
     * 잘못된 사용자 역할
     */
    public static String createInvalidUserRole() {
        return "INVALID_ROLE";
    }

    // ==================== Private Helper Methods ====================

    private static void setMemberId(Member member, Long id) {
        try {
            Field idField = Member.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(member, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set member id for test", e);
        }
    }

    private static void setMemberDeleted(Member member, boolean isDeleted) {
        try {
            Field deletedField = Member.class.getDeclaredField("isDeleted");
            deletedField.setAccessible(true);
            deletedField.set(member, isDeleted);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set member deleted status for test", e);
        }
    }

    private static void setSellerId(Seller seller, Long id) {
        try {
            Field idField = Seller.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(seller, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set seller id for test", e);
        }
    }

    private static void setSellerDeleted(Seller seller, boolean isDeleted) {
        try {
            Field deletedField = Seller.class.getDeclaredField("isDeleted");
            deletedField.setAccessible(true);
            deletedField.set(seller, isDeleted);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set seller deleted status for test", e);
        }
    }
} 