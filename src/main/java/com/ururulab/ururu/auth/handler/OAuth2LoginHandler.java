package com.ururulab.ururu.auth.handler;

import com.ururulab.ururu.auth.dto.response.SocialLoginResponse;
import com.ururulab.ururu.auth.jwt.JwtCookieHelper;
import com.ururulab.ururu.auth.service.OAuth2MemberService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

@Component
public final class OAuth2LoginHandler implements AuthenticationSuccessHandler, AuthenticationFailureHandler {

    private final OAuth2MemberService oAuth2MemberService;
    private final JwtCookieHelper jwtCookieHelper;
    private final String frontendBaseUrl;

    public OAuth2LoginHandler(
            final OAuth2MemberService oAuth2MemberService,
            final JwtCookieHelper jwtCookieHelper,
            @Value("${app.frontend.base-url}") final String frontendBaseUrl
    ) {
        this.oAuth2MemberService = oAuth2MemberService;
        this.jwtCookieHelper = jwtCookieHelper;
        this.frontendBaseUrl = validateFrontendBaseUrl(frontendBaseUrl);
    }

    @Override
    public void onAuthenticationSuccess(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Authentication authentication
    ) throws IOException, ServletException {
        try {
            if (!(authentication instanceof OAuth2AuthenticationToken token)) {
                throw new IllegalStateException("Unexpected OAuth2 authentication type");
            }
            final SocialLoginResponse tokens = oAuth2MemberService.login(
                    token.getAuthorizedClientRegistrationId(), token.getPrincipal().getAttributes()
            );
            jwtCookieHelper.setTokenCookies(response, tokens);
            invalidateSession(request);
        } catch (final Exception e) {
            jwtCookieHelper.clearTokenCookies(response);
            onAuthenticationFailure(
                    request,
                    response,
                    new OAuth2AuthenticationException(new OAuth2Error("oauth_processing_failed"), e)
            );
            return;
        }
        SecurityContextHolder.clearContext();
        response.sendRedirect(frontendBaseUrl + "/auth/success");
    }

    @Override
    public void onAuthenticationFailure(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final org.springframework.security.core.AuthenticationException exception
    ) throws IOException {
        invalidateSession(request);
        SecurityContextHolder.clearContext();
        response.sendRedirect(frontendBaseUrl + "/auth/callback?error=oauth_authentication_failed");
    }

    private void invalidateSession(final HttpServletRequest request) {
        final HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    private String validateFrontendBaseUrl(final String value) {
        final URI uri = URI.create(value == null ? "" : value.trim());
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null) {
            throw new IllegalArgumentException("app.frontend.base-url must be an absolute HTTP(S) URL");
        }
        final String normalized = uri.toString();
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }
}
