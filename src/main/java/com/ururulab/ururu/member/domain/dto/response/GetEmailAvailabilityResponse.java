package com.ururulab.ururu.member.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GetEmailAvailabilityResponse(
        @JsonProperty("is_available") boolean isAvailable
) {
    public static GetEmailAvailabilityResponse of(final boolean isAvailable) {
        return new GetEmailAvailabilityResponse(isAvailable);
    }
}
