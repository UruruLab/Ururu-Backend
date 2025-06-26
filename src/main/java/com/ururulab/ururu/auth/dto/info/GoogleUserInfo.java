package com.ururulab.ururu.auth.dto.info;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GoogleUserInfo(
        String id,
        String email,
        @JsonProperty("verified_email") boolean verifiedEmail,
        String name,
        @JsonProperty("given_name") String givenName,
        @JsonProperty("family_name") String familyName,
        String picture,
        String locale
) {

    public static GoogleUserInfo of(
            final String id,
            final String email,
            final boolean verifiedEmail,
            final String name,
            final String givenName,
            final String familyName,
            final String picture,
            final String locale
    ) {
        return new GoogleUserInfo(id, email, verifiedEmail, name,
                givenName, familyName, picture, locale);
    }
}