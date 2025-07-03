package com.ururulab.ururu.member.controller.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EmailAvailabilityResponse(
        @JsonProperty("is_available") boolean isAvailable
) {
    public static EmailAvailabilityResponse from(final boolean isAvailable) {
        return new EmailAvailabilityResponse(isAvailable);
    }
}
