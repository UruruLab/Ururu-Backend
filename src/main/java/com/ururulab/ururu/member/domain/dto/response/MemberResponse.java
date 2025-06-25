package com.ururulab.ururu.member.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ururulab.ururu.member.domain.entity.Member;

import java.time.LocalDate;

public record MemberResponse(
        Long id,
        String nickname,
        String email,
        String gender,
        LocalDate birth,
        String phone,
        @JsonProperty("profile_image") String profileImage,
        int point,
        @JsonProperty("is_available") Boolean isAvailable,
        String message
) {
    public static MemberResponse of(final Member member) {
        if (member == null) {
            throw new IllegalArgumentException("Member는 필수입니다.");
        }

        return new MemberResponse(
                member.getId(),
                member.getNickname(),
                member.getEmail(),
                member.getGender() != null ? member.getGender().name() : null,
                member.getBirth(),
                member.getPhone(),
                member.getProfileImage(),
                member.getPoint(),
                null,
                null
        );
    }

    public static MemberResponse ofAvailabilityCheck(boolean isAvailable) {
        return new MemberResponse(null, null, null, null, null, null, null, 0, isAvailable, null);
    }
}
