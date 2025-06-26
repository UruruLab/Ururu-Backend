package com.ururulab.ururu.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GoogleTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("expires_in") int expiresIn,
        @JsonProperty("refresh_token") String refreshToken,
        String scope,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("id_token") String idToken
) {

    public static GoogleTokenResponse of(
            final String accessToken,
            final int expiresIn,
            final String refreshToken,
            final String scope,
            final String tokenType,
            final String idToken
    ) {
        return new GoogleTokenResponse(accessToken, expiresIn, refreshToken,
                scope, tokenType, idToken);
    }
}