package com.ururulab.ururu.global.auth.oauth;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 카카오 OAuth2 설정 프로퍼티.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "oauth2.kakao")
public final class KakaoOAuthProperties {

    private static final String RESPONSE_TYPE = "code";
    private static final String GRANT_TYPE = "authorization_code";

    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String authorizationUri;
    private String tokenUri;
    private String userInfoUri;
    private String scope;

    public String buildAuthorizationUrl(final String state) {
        return String.format(
                "%s?client_id=%s&redirect_uri=%s&response_type=%s&scope=%s&state=%s",
                authorizationUri, clientId, redirectUri, RESPONSE_TYPE, scope, state
        );
    }

    public String buildTokenRequestBody(final String code) {
        return String.format(
                "grant_type=%s&client_id=%s&client_secret=%s&redirect_uri=%s&code=%s",
                GRANT_TYPE, clientId, clientSecret, redirectUri, code
        );
    }
}