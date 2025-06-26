package com.ururulab.ururu.member.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GetNicknameAvailabilityResponse(
        @JsonProperty("is_available") boolean isAvailable
) {
    public static GetNicknameAvailabilityResponse of(final boolean isAvailable){
        return new GetNicknameAvailabilityResponse(isAvailable);
    }
}
