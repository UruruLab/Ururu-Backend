package com.ururulab.ururu.member.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ururulab.ururu.global.domain.entity.enumerated.SkinTone;
import com.ururulab.ururu.global.domain.entity.enumerated.SkinType;
import com.ururulab.ururu.member.domain.entity.BeautyProfile;
import com.ururulab.ururu.member.domain.entity.Member;

public record MemberMyPageResponse(
        @JsonProperty("member_name") String memberName,
        @JsonProperty("profile_image") String profileImage,
        Integer points,
        @JsonProperty("skin_type") SkinType skinType,
        @JsonProperty("skin_tone")SkinTone skinTone
) {
    public static MemberMyPageResponse of (final Member member, final BeautyProfile beautyProfile){
        return new MemberMyPageResponse(
                member.getNickname(),
                member.getProfileImage(),
                member.getPoint(),
                beautyProfile != null ? beautyProfile.getSkinType() : null,
                beautyProfile != null ? beautyProfile.getSkinTone() : null
        );
    }

    public static MemberMyPageResponse from (final Member member){
        return new MemberMyPageResponse(
                member.getNickname(),
                member.getProfileImage(),
                member.getPoint(),
                null,
                null
        );
    }
}
