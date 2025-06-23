package com.ururulab.ururu.member.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ururulab.ururu.member.domain.entity.Member;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record MemberResponse(
        Long id,
        String nickname,
        String email,
        String gender,
        String birth,
        String phone,
        @JsonProperty("profile_image") String profileImage,
        int point,
        @JsonProperty("is_available") Boolean isAvailable,
        String message
) {
    private static final DateTimeFormatter BIRTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static MemberResponse of(final Member member) {
        if (member == null) {
            throw new IllegalArgumentException("Member는 필수입니다.");
        }

        return new MemberResponse(
                member.getId(),
                member.getNickname(),
                member.getEmail(),
                member.getGender() != null ? member.getGender().name() : null,
                formatBirthDate(member.getBirth()),
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


    private static String formatBirthDate(final LocalDateTime birth) {
        if (birth == null) {
            return null;
        }
        return birth.format(BIRTH_FORMATTER);
    }
}
