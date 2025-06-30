package com.ururulab.ururu.member.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EmailAvailabilityResponse(
        @JsonProperty("is_available") boolean isAvailable
) {
    public static EmailAvailabilityResponse of(final boolean isAvailable) {
        return new EmailAvailabilityResponse(isAvailable);
    }
}
