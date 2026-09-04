package com.ururulab.ururu.auth.controller;

import com.ururulab.ururu.auth.jwt.JwtCookieHelper;
import com.ururulab.ururu.auth.jwt.token.AccessTokenGenerator;
import com.ururulab.ururu.auth.service.JwtRefreshService;
import com.ururulab.ururu.auth.service.TokenValidator;
import com.ururulab.ururu.auth.service.UserInfoService;
import com.ururulab.ururu.global.domain.dto.ApiResponseFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private JwtCookieHelper jwtCookieHelper;
    @Mock private JwtRefreshService jwtRefreshService;
    @Mock private AccessTokenGenerator accessTokenGenerator;
    @Mock private UserInfoService userInfoService;
    @Mock private TokenValidator tokenValidator;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(
                jwtCookieHelper, jwtRefreshService, accessTokenGenerator, userInfoService, tokenValidator
        );
    }

    @Test
    void returnsSpringSecurityAuthorizationEndpoint() {
        final ResponseEntity<ApiResponseFormat<Object>> result = authController.getSocialAuthUrl("kakao");

        @SuppressWarnings("unchecked")
        final Map<String, Object> data = (Map<String, Object>) result.getBody().getData();
        assertThat(data).containsEntry("provider", "kakao")
                .containsEntry("authUrl", "/oauth2/authorization/kakao")
                .doesNotContainKey("state");
    }

    @Test
    void listsConfiguredProviderEntryPointsWithoutExposingState() {
        final AuthController.GetSocialProvidersResponse data =
                authController.getSocialProviders().getBody().getData();

        assertThat(data.providers()).extracting(AuthController.SocialProviderInfo::authUrl)
                .containsExactlyInAnyOrder(
                        "/oauth2/authorization/google",
                        "/oauth2/authorization/kakao"
                );
    }
}
