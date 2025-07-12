package com.ururulab.ururu.auth.service;

import com.ururulab.ururu.auth.constants.UserRole;
import com.ururulab.ururu.auth.constants.UserType;
import com.ururulab.ururu.auth.dto.request.SellerLoginRequest;
import com.ururulab.ururu.auth.dto.response.SocialLoginResponse;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.seller.domain.entity.Seller;
import com.ururulab.ururu.seller.service.SellerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.ururulab.ururu.auth.jwt.token.AccessTokenGenerator;
import com.ururulab.ururu.auth.jwt.token.RefreshTokenGenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("SellerAuthService 테스트")
class SellerAuthServiceTest {
    @Mock
    private SellerService sellerService;
    @Mock
    private AccessTokenGenerator accessTokenGenerator;
    @Mock
    private RefreshTokenGenerator refreshTokenGenerator;
    @Mock
    private JwtRefreshService jwtRefreshService;
    @Mock
    private PasswordEncoder passwordEncoder;
    private SellerAuthService sellerAuthService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        sellerAuthService = new SellerAuthService(
                sellerService,
                accessTokenGenerator,
                refreshTokenGenerator,
                jwtRefreshService,
                passwordEncoder
        );
    }

    @Test
    @DisplayName("정상적으로 판매자 로그인 성공")
    void login_success() {
        // Given
        SellerLoginRequest request = SellerLoginRequest.of("seller@ururu.com", "pw");
        Seller seller = mock(Seller.class);
        
        // SellerService Mock 설정
        when(sellerService.findByEmail("seller@ururu.com")).thenReturn(seller);
        when(seller.getPassword()).thenReturn("encodedPassword");
        when(passwordEncoder.matches("pw", "encodedPassword")).thenReturn(true);
        when(seller.getIsDeleted()).thenReturn(false);
        when(seller.getId()).thenReturn(1L);
        when(seller.getEmail()).thenReturn("seller@ururu.com");
        when(seller.getName()).thenReturn("브랜드");
        when(seller.getImage()).thenReturn("img");
        
        // JWT 토큰 생성 Mock 설정
        when(accessTokenGenerator.generateAccessToken(1L, "seller@ururu.com", UserRole.SELLER, UserType.SELLER)).thenReturn("access");
        when(refreshTokenGenerator.generateRefreshToken(1L, UserType.SELLER)).thenReturn("refresh");
        when(accessTokenGenerator.getExpirySeconds()).thenReturn(3600L);
        
        // JwtRefreshService Mock 설정
        doNothing().when(jwtRefreshService).storeRefreshToken(anyLong(), anyString(), anyString());

        // When
        SocialLoginResponse response = sellerAuthService.login(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
        assertThat(response.expiresIn()).isEqualTo(3600L);
        assertThat(response.memberInfo().memberId()).isEqualTo(1L);
        assertThat(response.memberInfo().email()).isEqualTo("seller@ururu.com");
        assertThat(response.memberInfo().nickname()).isEqualTo("브랜드");
        assertThat(response.memberInfo().userType()).isEqualTo(UserType.SELLER.getValue());
        
        // 실제 구현과 일치하는 검증
        verify(sellerService).findByEmail("seller@ururu.com");
        verify(passwordEncoder).matches("pw", "encodedPassword");
        verify(jwtRefreshService).storeRefreshToken(eq(1L), eq(UserType.SELLER.getValue()), eq("refresh"));
        verify(accessTokenGenerator).generateAccessToken(eq(1L), eq("seller@ururu.com"), eq(UserRole.SELLER), eq(UserType.SELLER));
        verify(refreshTokenGenerator).generateRefreshToken(eq(1L), eq(UserType.SELLER));
    }

    @Test
    @DisplayName("비밀번호 불일치 시 예외 발생")
    void login_invalidPassword_throws() {
        // Given
        SellerLoginRequest request = SellerLoginRequest.of("seller@ururu.com", "pw");
        Seller seller = mock(Seller.class);
        
        when(sellerService.findByEmail("seller@ururu.com")).thenReturn(seller);
        when(seller.getPassword()).thenReturn("encodedPassword");
        when(passwordEncoder.matches("pw", "encodedPassword")).thenReturn(false);
        when(seller.getIsDeleted()).thenReturn(false);
        
        // When & Then
        assertThatThrownBy(() -> sellerAuthService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_LOGIN_CREDENTIALS);
    }

    @Test
    @DisplayName("삭제된 계정 로그인 시 예외 발생")
    void login_deletedAccount_throws() {
        // Given
        SellerLoginRequest request = SellerLoginRequest.of("seller@ururu.com", "pw");
        Seller seller = mock(Seller.class);
        
        when(sellerService.findByEmail("seller@ururu.com")).thenReturn(seller);
        when(seller.getPassword()).thenReturn("encodedPassword");
        when(passwordEncoder.matches("pw", "encodedPassword")).thenReturn(true);
        when(seller.getIsDeleted()).thenReturn(true);
        
        // When & Then
        assertThatThrownBy(() -> sellerAuthService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INACTIVE_ACCOUNT);
    }

    @Test
    @DisplayName("존재하지 않는 판매자 로그인 시 예외 발생")
    void login_nonExistentSeller_throws() {
        // Given
        SellerLoginRequest request = SellerLoginRequest.of("nonexistent@ururu.com", "pw");
        
        when(sellerService.findByEmail("nonexistent@ururu.com")).thenThrow(new BusinessException(ErrorCode.SELLER_NOT_FOUND));
        
        // When & Then
        assertThatThrownBy(() -> sellerAuthService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SELLER_NOT_FOUND);
    }

    @Test
    @DisplayName("로그아웃 성공 - jwtRefreshService.logout 호출")
    void logout_success() {
        // Given
        String authorization = "Bearer token";
        
        // When
        sellerAuthService.logout(authorization);
        
        // Then
        verify(jwtRefreshService).logout(authorization);
    }

    @Test
    @DisplayName("토큰으로 로그아웃 성공 - jwtRefreshService.logoutWithToken 호출")
    void logoutWithToken_success() {
        // Given
        String accessToken = "access";
        
        // When
        sellerAuthService.logoutWithToken(accessToken);
        
        // Then
        verify(jwtRefreshService).logoutWithToken(accessToken);
    }
} 