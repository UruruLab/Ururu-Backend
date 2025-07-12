package com.ururulab.ururu.auth.service;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.member.domain.entity.enumerated.SocialProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@DisplayName("SocialLoginServiceFactory 테스트")
class SocialLoginServiceFactoryTest {
    @Mock
    private SocialLoginService googleLoginService;
    @Mock
    private SocialLoginService kakaoLoginService;
    private SocialLoginServiceFactory factory;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(googleLoginService.getProvider()).thenReturn(SocialProvider.GOOGLE);
        when(kakaoLoginService.getProvider()).thenReturn(SocialProvider.KAKAO);
        factory = new SocialLoginServiceFactory(List.of(googleLoginService, kakaoLoginService));
    }

    @Test
    @DisplayName("GOOGLE provider로 서비스 반환 성공")
    void getService_google_success() {
        SocialLoginService service = factory.getService(SocialProvider.GOOGLE);
        assertThat(service).isEqualTo(googleLoginService);
    }

    @Test
    @DisplayName("KAKAO provider로 서비스 반환 성공")
    void getService_kakao_success() {
        SocialLoginService service = factory.getService(SocialProvider.KAKAO);
        assertThat(service).isEqualTo(kakaoLoginService);
    }

    @Test
    @DisplayName("null provider로 예외 발생")
    void getService_nullProvider_throws() {
        assertThatThrownBy(() -> factory.getService(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("소셜 제공자는 필수");
    }

    @Test
    @DisplayName("미지원 provider로 예외 발생")
    void getService_unsupportedProvider_throws() {
        when(googleLoginService.getProvider()).thenReturn(SocialProvider.GOOGLE);
        when(kakaoLoginService.getProvider()).thenReturn(SocialProvider.KAKAO);
        SocialLoginServiceFactory singleFactory = new SocialLoginServiceFactory(List.of(googleLoginService));
        assertThatThrownBy(() -> singleFactory.getService(SocialProvider.KAKAO))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNSUPPORTED_SOCIAL_PROVIDER);
    }
} 