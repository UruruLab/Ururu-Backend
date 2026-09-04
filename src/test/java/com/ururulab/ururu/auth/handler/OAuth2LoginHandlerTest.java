package com.ururulab.ururu.auth.handler;

import com.ururulab.ururu.auth.dto.response.SocialLoginResponse;
import com.ururulab.ururu.auth.jwt.JwtCookieHelper;
import com.ururulab.ururu.auth.service.OAuth2MemberService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuth2LoginHandlerTest {

    @Mock private OAuth2MemberService oAuth2MemberService;
    @Mock private JwtCookieHelper jwtCookieHelper;

    @Test
    void issuesLocalCookiesAndRedirectsWithoutPuttingTokensInTheUrl() throws Exception {
        final OAuth2LoginHandler handler = new OAuth2LoginHandler(
                oAuth2MemberService, jwtCookieHelper, "https://www.ururu.shop"
        );
        final Map<String, Object> attributes = Map.of("sub", "google-id");
        final var principal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")), attributes, "sub"
        );
        final var authentication = new OAuth2AuthenticationToken(
                principal, principal.getAuthorities(), "google"
        );
        final SocialLoginResponse tokens = SocialLoginResponse.of(
                "access", "refresh", 900L,
                SocialLoginResponse.MemberInfo.of(1L, "member@example.com", "member", null, "MEMBER")
        );
        when(oAuth2MemberService.login("google", attributes)).thenReturn(tokens);
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession();
        final MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(jwtCookieHelper).setTokenCookies(response, tokens);
        assertThat(response.getRedirectedUrl()).isEqualTo("https://www.ururu.shop/auth/success");
        assertThat(response.getRedirectedUrl()).doesNotContain("access", "refresh");
    }
}
