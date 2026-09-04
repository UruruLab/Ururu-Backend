package com.ururulab.ururu.auth.controller;

import com.ururulab.ururu.auth.constants.AuthConstants;
import com.ururulab.ururu.auth.dto.response.SocialLoginResponse;
import com.ururulab.ururu.auth.jwt.JwtCookieHelper;
import com.ururulab.ururu.auth.jwt.JwtTokenProvider;
import com.ururulab.ururu.auth.jwt.token.AccessTokenGenerator;
import com.ururulab.ururu.auth.service.JwtRefreshService;
import com.ururulab.ururu.auth.service.SecurityLoggingService;
import com.ururulab.ururu.auth.service.SocialLoginService;
import com.ururulab.ururu.auth.service.SocialLoginServiceFactory;
import com.ururulab.ururu.auth.service.TokenValidator;
import com.ururulab.ururu.auth.service.UserInfoService;
import com.ururulab.ururu.global.domain.dto.ApiResponseFormat;
import com.ururulab.ururu.member.domain.entity.enumerated.SocialProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.view.RedirectView;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private SocialLoginServiceFactory socialLoginServiceFactory;
    @Mock private JwtCookieHelper jwtCookieHelper;
    @Mock private JwtRefreshService jwtRefreshService;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private AccessTokenGenerator accessTokenGenerator;
    @Mock private UserInfoService userInfoService;
    @Mock private TokenValidator tokenValidator;
    @Mock private Environment environment;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private SecurityLoggingService securityLoggingService;
    @Mock private SocialLoginService socialLoginService;
    @Mock private ValueOperations<String, String> valueOperations;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(
                socialLoginServiceFactory,
                jwtCookieHelper,
                jwtRefreshService,
                jwtTokenProvider,
                accessTokenGenerator,
                userInfoService,
                tokenValidator,
                environment,
                redisTemplate,
                securityLoggingService
        );
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void authUrlAndResponseUseTheSameStoredState() {
        final AtomicReference<String> generatedState = new AtomicReference<>();
        when(socialLoginServiceFactory.getService(SocialProvider.KAKAO)).thenReturn(socialLoginService);
        when(socialLoginService.getAuthorizationUrl(anyString())).thenAnswer(invocation -> {
            final String state = invocation.getArgument(0);
            generatedState.set(state);
            return "https://kauth.kakao.com/oauth/authorize?state=" + state;
        });
        when(environment.getProperty("oauth2.kakao.redirect-uri", "NOT_SET"))
                .thenReturn("http://localhost:8080/api/auth/oauth/kakao");

        final ResponseEntity<ApiResponseFormat<Object>> result = authController.getSocialAuthUrl("kakao");

        @SuppressWarnings("unchecked")
        final Map<String, Object> data = (Map<String, Object>) result.getBody().getData();
        final String state = (String) data.get("state");
        assertThat(state).isEqualTo(generatedState.get());
        assertThat(data.get("authUrl")).isEqualTo(
                "https://kauth.kakao.com/oauth/authorize?state=" + state
        );
        verify(valueOperations).set(
                AuthConstants.OAUTH_STATE_KEY_PREFIX + state,
                SocialProvider.KAKAO.name(),
                Duration.ofSeconds(AuthConstants.OAUTH_CODE_TTL_SECONDS)
        );
    }

    @Test
    void callbackRejectsUnknownStateBeforeUsingAuthorizationCode() {
        when(valueOperations.getAndDelete(AuthConstants.OAUTH_STATE_KEY_PREFIX + "unknown"))
                .thenReturn(null);
        when(environment.getProperty("app.frontend.base-url")).thenReturn("http://localhost:3000");

        final RedirectView result = authController.handleKakaoCallback(
                "authorization-code",
                "unknown",
                null,
                new MockHttpServletResponse()
        );

        assertThat(result.getUrl()).contains("reason=invalid_state");
        verify(socialLoginServiceFactory, never()).getService(SocialProvider.KAKAO);
        verify(valueOperations, never()).setIfAbsent(anyString(), anyString(), eq(Duration.ofSeconds(
                AuthConstants.OAUTH_CODE_TTL_SECONDS
        )));
    }

    @Test
    void callbackConsumesStateAndKeepsAuthorizationCodeMarkedAsUsed() {
        final SocialLoginResponse loginResponse = SocialLoginResponse.of(
                "access-token",
                "refresh-token",
                900L,
                SocialLoginResponse.MemberInfo.of(1L, "member@example.com", "member", null, "MEMBER")
        );
        when(valueOperations.getAndDelete(AuthConstants.OAUTH_STATE_KEY_PREFIX + "valid-state"))
                .thenReturn(SocialProvider.KAKAO.name());
        when(valueOperations.setIfAbsent(
                AuthConstants.OAUTH_CODE_KEY_PREFIX + "authorization-code",
                "1",
                Duration.ofSeconds(AuthConstants.OAUTH_CODE_TTL_SECONDS)
        )).thenReturn(true);
        when(socialLoginServiceFactory.getService(SocialProvider.KAKAO)).thenReturn(socialLoginService);
        when(socialLoginService.processLogin("authorization-code")).thenReturn(loginResponse);
        when(environment.getProperty("app.frontend.base-url")).thenReturn("http://localhost:3000");
        when(securityLoggingService.maskEmail("member@example.com")).thenReturn("m***@example.com");

        final MockHttpServletResponse response = new MockHttpServletResponse();
        final RedirectView result = authController.handleKakaoCallback(
                "authorization-code",
                "valid-state",
                null,
                response
        );

        assertThat(result.getUrl()).isEqualTo("http://localhost:3000/auth/success");
        verify(jwtCookieHelper).setAccessTokenCookie(response, "access-token");
        verify(jwtCookieHelper).setRefreshTokenCookie(response, "refresh-token");
        verify(redisTemplate, never()).delete(anyString());
    }
}
