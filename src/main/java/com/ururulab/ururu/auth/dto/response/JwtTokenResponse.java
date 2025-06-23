package com.ururulab.ururu.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JWT 토큰 발급 응답 DTO.
 */
public record JwtTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") Long expiresIn
) {
    private static final String BEARER_TYPE = "Bearer";

    public static JwtTokenResponse of(final String accessToken, final String refreshToken, final Long expiresIn) {
        validateTokens(accessToken, refreshToken, expiresIn);
        return new JwtTokenResponse(accessToken, refreshToken, BEARER_TYPE, expiresIn);
    }

    public static JwtTokenResponse accessTokenOnly(final String accessToken, final Long expiresIn) {
        validateAccessToken(accessToken);
        validateExpiresIn(expiresIn);
        return new JwtTokenResponse(accessToken, null, BEARER_TYPE, expiresIn);
    }

    private static void validateTokens(final String accessToken, final String refreshToken, final Long expiresIn) {
        validateAccessToken(accessToken);
        validateRefreshToken(refreshToken);
        validateExpiresIn(expiresIn);
    }

    private static void validateAccessToken(final String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("액세스 토큰은 필수입니다.");
        }
    }

    private static void validateRefreshToken(final String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("리프레시 토큰은 필수입니다.");
        }
    }

    private static void validateExpiresIn(final Long expiresIn) {
        if (expiresIn == null || expiresIn <= 0) {
            throw new IllegalArgumentException("만료 시간은 0보다 커야 합니다.");
        }
    }
}