package com.ururulab.ururu.member.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ururulab.ururu.member.domain.entity.Member;

import java.time.Instant;
import java.time.LocalDate;

public record UpdateMyProfileResponse(
        Long id,
        String nickname,
        String gender,
        LocalDate birth,
        String phone,
        @JsonProperty("profile_image") String profileImage,
        @JsonProperty("updated_at") Instant updatedAt
) {
    public static UpdateMyProfileResponse from(final Member member) {
        return new UpdateMyProfileResponse(
                member.getId(),
                member.getNickname(),
                member.getGender() != null ? member.getGender().name() : null,
                member.getBirth(),
                member.getPhone(),
                member.getProfileImage(),
                member.getUpdatedAt()
        );
    }
}
