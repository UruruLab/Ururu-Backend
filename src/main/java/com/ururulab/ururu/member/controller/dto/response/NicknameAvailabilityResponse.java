package com.ururulab.ururu.member.controller.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NicknameAvailabilityResponse(
        @JsonProperty("is_available") boolean isAvailable
) {
    public static NicknameAvailabilityResponse from(final boolean isAvailable){
        return new NicknameAvailabilityResponse(isAvailable);
    }
}
