package com.ururulab.ururu.member.service;

import com.ururulab.ururu.auth.dto.info.SocialMemberInfo;
import com.ururulab.ururu.global.domain.entity.enumerated.Gender;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.entity.enumerated.Role;
import com.ururulab.ururu.member.domain.entity.enumerated.SocialProvider;

import java.time.Instant;

public class MemberTestFixture {

    public static SocialMemberInfo createSocialMemberInfo() {
        return new SocialMemberInfo(
                "testuser",
                "test@example.com",
                "social123",
                "",
                SocialProvider.GOOGLE
        );
    }

    public static Member createMember(Long id, String nickname, String email){
        return Member.of(
                nickname,
                email,
                SocialProvider.GOOGLE,
                "social123",
                Gender.FEMALE,
                Instant.parse("1990-01-01T00:00:00Z"),
                "01012345678",
                null,
                Role.NORMAL
        );
    }
}
