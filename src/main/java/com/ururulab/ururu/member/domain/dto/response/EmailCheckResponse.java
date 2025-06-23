package com.ururulab.ururu.member.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EmailCheckResponse(
        @JsonProperty("is_available") boolean isAvailable
) {
    public static EmailCheckResponse of(final boolean isAvailable){
        return new EmailCheckResponse(isAvailable);
    }
}
