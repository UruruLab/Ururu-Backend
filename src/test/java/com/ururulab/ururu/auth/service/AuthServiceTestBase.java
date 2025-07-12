package com.ururulab.ururu.auth.service;

import com.ururulab.ururu.auth.AuthTestFixture;
import com.ururulab.ururu.auth.jwt.JwtProperties;
import com.ururulab.ururu.auth.jwt.JwtTokenProvider;
import com.ururulab.ururu.auth.storage.RefreshTokenStorage;
import com.ururulab.ururu.auth.storage.TokenBlacklistStorage;
import com.ururulab.ururu.member.domain.repository.MemberRepository;
import com.ururulab.ururu.seller.domain.repository.SellerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    /**
     * 테스트용 Member를 생성하고 Repository Mock에 설정합니다.
     */
    protected void givenMemberExists(Long memberId, String nickname, String email) {
        // Member 생성 및 Repository Mock 설정 로직
        // 구체적인 구현은 각 테스트 클래스에서 구현
    }

    /**
     * 테스트용 Seller를 생성하고 Repository Mock에 설정합니다.
     */
    protected void givenSellerExists(Long sellerId, String email, String name) {
        // Seller 생성 및 Repository Mock 설정 로직
        // 구체적인 구현은 각 테스트 클래스에서 구현
    }

    /**
     * 테스트용 Refresh Token을 Storage Mock에 설정합니다.
     */
    protected void givenRefreshTokenExists(String userType, Long userId, String tokenId, String refreshToken) {
        // Refresh Token Storage Mock 설정 로직
        // 구체적인 구현은 각 테스트 클래스에서 구현
    }

    /**
     * 테스트용 토큰을 Blacklist Mock에 설정합니다.
     */
    protected void givenTokenIsBlacklisted(String tokenId) {
        // Token Blacklist Storage Mock 설정 로직
        // 구체적인 구현은 각 테스트 클래스에서 구현
    }
} 