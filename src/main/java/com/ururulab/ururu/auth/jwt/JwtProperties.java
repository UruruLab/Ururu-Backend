package com.ururulab.ururu.auth.jwt;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * JWT 토큰 관련 설정 프로퍼티.
 */
@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "jwt")
public final class JwtProperties {

    @NotBlank
    @Size(min = 32)
    private String secret;
    @Positive
    private long accessTokenExpiry;
    @Positive
    private long refreshTokenExpiry;
    @NotBlank
    private String issuer;
    @NotBlank
    private String audience;
}
