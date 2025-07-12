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
        SellerLoginRequest request = SellerLoginRequest.of("seller@ururu.com", "pw");
        Seller seller = mock(Seller.class);
        when(sellerService.findByEmail("seller@ururu.com")).thenReturn(seller);
        when(passwordEncoder.matches("pw", seller.getPassword())).thenReturn(true);
        when(seller.getIsDeleted()).thenReturn(false);
        when(seller.getId()).thenReturn(1L);
        when(seller.getEmail()).thenReturn("seller@ururu.com");
        when(seller.getName()).thenReturn("브랜드");
        when(seller.getImage()).thenReturn("img");
        when(accessTokenGenerator.generateAccessToken(1L, "seller@ururu.com", UserRole.SELLER, UserType.SELLER)).thenReturn("access");
        when(refreshTokenGenerator.generateRefreshToken(1L, UserType.SELLER)).thenReturn("refresh");
        when(accessTokenGenerator.getExpirySeconds()).thenReturn(3600L);

        SocialLoginResponse response = sellerAuthService.login(request);
        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
        assertThat(response.memberInfo().email()).isEqualTo("seller@ururu.com");
        verify(jwtRefreshService).storeRefreshToken(1L, UserType.SELLER.getValue(), "refresh");
    }

    @Test
    @DisplayName("비밀번호 불일치 시 예외 발생")
    void login_invalidPassword_throws() {
        SellerLoginRequest request = SellerLoginRequest.of("seller@ururu.com", "pw");
        Seller seller = mock(Seller.class);
        when(sellerService.findByEmail("seller@ururu.com")).thenReturn(seller);
        when(passwordEncoder.matches("pw", seller.getPassword())).thenReturn(false);
        when(seller.getIsDeleted()).thenReturn(false);
        assertThatThrownBy(() -> sellerAuthService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_LOGIN_CREDENTIALS);
    }

    @Test
    @DisplayName("삭제된 계정 로그인 시 예외 발생")
    void login_deletedAccount_throws() {
        SellerLoginRequest request = SellerLoginRequest.of("seller@ururu.com", "pw");
        Seller seller = mock(Seller.class);
        when(sellerService.findByEmail("seller@ururu.com")).thenReturn(seller);
        when(passwordEncoder.matches("pw", seller.getPassword())).thenReturn(true);
        when(seller.getIsDeleted()).thenReturn(true);
        assertThatThrownBy(() -> sellerAuthService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INACTIVE_ACCOUNT);
    }

    @Test
    @DisplayName("로그아웃 성공 - jwtRefreshService.logout 호출")
    void logout_success() {
        sellerAuthService.logout("Bearer token");
        verify(jwtRefreshService).logout("Bearer token");
    }

    @Test
    @DisplayName("토큰으로 로그아웃 성공 - jwtRefreshService.logoutWithToken 호출")
    void logoutWithToken_success() {
        sellerAuthService.logoutWithToken("access");
        verify(jwtRefreshService).logoutWithToken("access");
    }
} 