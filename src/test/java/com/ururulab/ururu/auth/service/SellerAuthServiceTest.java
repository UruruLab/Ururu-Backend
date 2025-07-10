package com.ururulab.ururu.auth.service;

import com.ururulab.ururu.auth.dto.request.SellerLoginRequest;
import com.ururulab.ururu.auth.dto.response.SocialLoginResponse;
import com.ururulab.ururu.auth.jwt.JwtProperties;
import com.ururulab.ururu.auth.jwt.JwtTokenProvider;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.seller.domain.entity.Seller;
import com.ururulab.ururu.seller.service.SellerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SellerAuthServiceTest {

    @Mock
    private SellerService sellerService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private JwtRefreshService jwtRefreshService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private SellerAuthService sellerAuthService;

    private Seller testSeller;
    private SellerLoginRequest validRequest;

    @BeforeEach
    void setUp() {
        testSeller = Seller.of(
                "테스트브랜드",
                "테스트사업자",
                "홍길동",
                "1234567890",
                "test@example.com",
                "encodedPassword",
                "010-1234-5678",
                null,
                "서울시 강남구",
                "테헤란로 123",
                "2024-서울강남-1234"
        );

        validRequest = SellerLoginRequest.of("test@example.com", "password123");
    }

    @Test
    @DisplayName("유효한 판매자 로그인 요청 시 성공")
    void login_WithValidCredentials_ShouldSucceed() {
        // given
        when(sellerService.findByEmail("test@example.com")).thenReturn(testSeller);
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(any(), anyString(), anyString(), anyString()))
                .thenReturn("access.token.here");
        when(jwtTokenProvider.generateRefreshToken(any(), anyString()))
                .thenReturn("refresh.token.here");
        when(jwtProperties.getAccessTokenExpiry()).thenReturn(3600L);

        // when
        SocialLoginResponse response = sellerAuthService.login(validRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("access.token.here");
        assertThat(response.refreshToken()).isEqualTo("refresh.token.here");
        assertThat(response.memberInfo().email()).isEqualTo("test@example.com");
        assertThat(response.memberInfo().userType()).isEqualTo("SELLER");
    }

    @Test
    @DisplayName("잘못된 비밀번호로 로그인 시 실패")
    void login_WithInvalidPassword_ShouldFail() {
        // given
        when(sellerService.findByEmail("test@example.com")).thenReturn(testSeller);
        when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

        SellerLoginRequest invalidRequest = SellerLoginRequest.of("test@example.com", "wrongpassword");

        // when & then
        assertThatThrownBy(() -> sellerAuthService.login(invalidRequest))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_LOGIN_CREDENTIALS);
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 로그인 시 실패")
    void login_WithNonExistentEmail_ShouldFail() {
        // given
        when(sellerService.findByEmail("nonexistent@example.com"))
                .thenThrow(new BusinessException(ErrorCode.SELLER_NOT_FOUND));

        SellerLoginRequest invalidRequest = SellerLoginRequest.of("nonexistent@example.com", "password123");

        // when & then
        assertThatThrownBy(() -> sellerAuthService.login(invalidRequest))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SELLER_NOT_FOUND);
    }
} 