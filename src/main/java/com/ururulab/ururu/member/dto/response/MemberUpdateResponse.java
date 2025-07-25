package com.ururulab.ururu.member.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ururulab.ururu.member.domain.entity.Member;

import java.time.Instant;
import java.time.LocalDate;

public record MemberUpdateResponse(
        Long id,
        String email,
        String nickname,
        String gender,
        LocalDate birth,
        String phone,
        @JsonProperty("profile_image") String profileImage,
        @JsonProperty("updated_at") Instant updatedAt
) {
    public static MemberUpdateResponse from (final Member member){
        return new MemberUpdateResponse(
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
