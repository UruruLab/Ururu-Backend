package com.ururulab.ururu.auth.jwt;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class JwtCookieHelperTest {

    @Test
    void appliesConfiguredCookiePolicy() {
        final JwtProperties properties = new JwtProperties();
        properties.setAccessTokenExpiry(900);

        final MockHttpServletResponse localResponse = new MockHttpServletResponse();
        new JwtCookieHelper(properties, "")
                .setAccessTokenCookie(localResponse, "local-token");

        assertThat(localResponse.getHeader("Set-Cookie"))
                .contains("HttpOnly", "SameSite=Lax")
                .doesNotContain("Secure", "Domain=");

        final MockHttpServletResponse prodResponse = new MockHttpServletResponse();
        new JwtCookieHelper(properties, "ururu.shop")
                .setAccessTokenCookie(prodResponse, "prod-token");

        assertThat(prodResponse.getHeader("Set-Cookie"))
                .contains("HttpOnly", "Secure", "Domain=ururu.shop", "SameSite=None");
    }
}
