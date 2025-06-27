package com.ururulab.ururu.auth.oauth;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "oauth2.google")
public final class GoogleOAuthProperties {

    private static final String RESPONSE_TYPE = "code";
    private static final String GRANT_TYPE = "authorization_code";
    private static final String ACCESS_TYPE = "offline";
    private static final String PROMPT = "consent";

    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String authorizationUri;
    private String tokenUri;
    private String memberInfoUri;
    private String scope;

    public String buildAuthorizationUrl(final String state) {
        if (state == null || state.isBlank()) {
            throw new IllegalArgumentException("state 매개변수는 필수입니다");
        }
        return UriComponentsBuilder.fromUriString(authorizationUri)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", RESPONSE_TYPE)
                .queryParam("scope", scope)
                .queryParam("state", state)
                .queryParam("access_type", ACCESS_TYPE)
                .queryParam("prompt", PROMPT)
                .build()
                .toUriString();
    }

    public String buildTokenRequestBody(final String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("인증 코드는 필수입니다");
        }
        try {
            return String.format(
                    "grant_type=%s&client_id=%s&client_secret=%s&redirect_uri=%s&code=%s",
                    URLEncoder.encode(GRANT_TYPE, StandardCharsets.UTF_8),
                    URLEncoder.encode(clientId, StandardCharsets.UTF_8),
                    URLEncoder.encode(clientSecret, StandardCharsets.UTF_8),
                    URLEncoder.encode(redirectUri, StandardCharsets.UTF_8),
                    URLEncoder.encode(code, StandardCharsets.UTF_8)
            );
        } catch (final Exception e) {
            throw new IllegalStateException("URL 인코딩 실패", e);
        }
    }
}