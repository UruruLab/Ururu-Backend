package com.ururulab.ururu.member.controller.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ururulab.ururu.member.domain.entity.Member;

import java.time.Instant;
import java.time.LocalDate;

public record MemberGetResponse(
        Long id,
        String email,
        String nickname,
        String gender,
        LocalDate birth,
        String phone,
        @JsonProperty("profile_image") String profileImage,
        @JsonProperty("social_provider") String socialProvider,
        String role,
        int point,
        @JsonProperty("is_deleted") boolean isDeleted,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt
) {
    public static MemberGetResponse from(final Member member) {
        return new MemberGetResponse(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getGender() != null ? member.getGender().name() : null,
                member.getBirth(),
                member.getPhone(),
                member.getProfileImage(),
                member.getSocialProvider().name(),
                member.getRole().name(),
                member.getPoint(),
                member.isDeleted(),
                member.getCreatedAt(),
                member.getUpdatedAt()
        );
    }
}
