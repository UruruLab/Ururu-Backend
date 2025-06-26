package com.ururulab.ururu.member.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ururulab.ururu.member.domain.entity.Member;

import java.time.LocalDate;
import java.time.ZonedDateTime;

public record UpdateMemberResponse(
        Long id,
        String email,
        String nickname,
        String gender,
        LocalDate birth,
        String phone,
        @JsonProperty("profile_image") String profileImage,
        @JsonProperty("updated_at") ZonedDateTime updatedAt
) {
    public static UpdateMemberResponse from (final Member member){
        return new UpdateMemberResponse(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getGender() != null ? member.getGender().name() : null,
                member.getBirth(),
                member.getPhone(),
                member.getProfileImage(),
                member.getUpdatedAt()
        );
    }
}
