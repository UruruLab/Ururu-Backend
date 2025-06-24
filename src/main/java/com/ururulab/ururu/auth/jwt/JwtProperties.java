package com.ururulab.ururu.auth.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 토큰 관련 설정 프로퍼티.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public final class JwtProperties {

    private String secret;
    private long accessTokenExpiry = 3600L;
    private long refreshTokenExpiry = 1209600L;
    private String issuer = "ururu-backend";
    private String audience = "ururu-client";
}