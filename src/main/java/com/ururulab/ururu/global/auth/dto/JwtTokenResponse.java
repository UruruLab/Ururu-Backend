package com.ururulab.ururu.global.auth.jwt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JWT 토큰 발급 응답 DTO.
 *
 * <p>로그인 성공 시 클라이언트에게 전달되는 JWT 토큰 정보</p>
 */
public record JwtTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") Long expiresIn
) {
    private static final String BEARER_TYPE = "Bearer";

    /** Bearer 타입 JWT 토큰 응답 생성 (access + refresh) */
    public static JwtTokenResponse of(String accessToken, String refreshToken, Long expiresIn) {
        if (accessToken == null || accessToken.isBlank())
            throw new IllegalArgumentException("액세스 토큰은 필수입니다.");
        if (refreshToken == null || refreshToken.isBlank())
            throw new IllegalArgumentException("리프레시 토큰은 필수입니다.");
        if (expiresIn == null || expiresIn <= 0)
            throw new IllegalArgumentException("만료 시간은 0보다 커야 합니다.");
        return new JwtTokenResponse(accessToken, refreshToken, BEARER_TYPE, expiresIn);
    }

    /** 액세스 토큰만 포함하는 응답 (리프레시 토큰은 null) */
    public static JwtTokenResponse accessTokenOnly(String accessToken, Long expiresIn) {
        if (accessToken == null || accessToken.isBlank())
            throw new IllegalArgumentException("액세스 토큰은 필수입니다.");
        if (expiresIn == null || expiresIn <= 0)
            throw new IllegalArgumentException("만료 시간은 0보다 커야 합니다.");
        return new JwtTokenResponse(accessToken, null, BEARER_TYPE, expiresIn);
    }
}
