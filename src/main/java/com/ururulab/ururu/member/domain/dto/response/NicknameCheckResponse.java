package com.ururulab.ururu.member.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NicknameCheckResponse(
        @JsonProperty("is_available") boolean isAvailable
) {
    public static NicknameCheckResponse of(final boolean isAvailable){
        return new NicknameCheckResponse(isAvailable);
    }
}
