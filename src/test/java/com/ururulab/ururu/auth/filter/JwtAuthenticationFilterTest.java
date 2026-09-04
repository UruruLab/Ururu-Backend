package com.ururulab.ururu.auth.filter;

import com.ururulab.ururu.auth.service.TokenValidator;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesPlatformAdminOnlyAsAdmin() throws Exception {
        final TokenValidator validator = mock(TokenValidator.class);
        when(validator.validateAccessToken("access-token"))
                .thenReturn(TokenValidator.TokenValidationResult.of(1L, "MEMBER", "jti", "ADMIN"));

        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/members/me");
        request.addHeader("Authorization", "Bearer access-token");
        new JwtAuthenticationFilter(validator).doFilter(
                request, new MockHttpServletResponse(), mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void authenticatesMemberOnlyAsMember() throws Exception {
        final TokenValidator validator = mock(TokenValidator.class);
        when(validator.validateAccessToken("access-token"))
                .thenReturn(TokenValidator.TokenValidationResult.of(1L, "MEMBER", "jti", "MEMBER"));

        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/members/me");
        request.addHeader("Authorization", "Bearer access-token");
        new JwtAuthenticationFilter(validator).doFilter(
                request, new MockHttpServletResponse(), mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_MEMBER");
    }
}
