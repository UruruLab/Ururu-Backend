package com.ururulab.ururu.member.service;

import com.ururulab.ururu.global.domain.entity.enumerated.Gender;
import com.ururulab.ururu.global.domain.entity.enumerated.SkinTone;
import com.ururulab.ururu.global.domain.entity.enumerated.SkinType;
import com.ururulab.ururu.member.domain.entity.BeautyProfile;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.entity.enumerated.Role;
import com.ururulab.ururu.member.domain.entity.enumerated.SocialProvider;
import com.ururulab.ururu.member.dto.request.BeautyProfileRequest;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Arrays;

public class BeautyProfileTestFixture {

    public static Member createMember(Long id) {
        Member member = Member.of(
                "testUser",
                "test@example.com",
                SocialProvider.GOOGLE,
                "social123",
                Gender.FEMALE,
                LocalDate.parse("1990-01-01"),
                "01012345678",
                "profile.jpg",
                Role.NORMAL
        );
        setMemberId(member, id);
        return member;
    }

    public static BeautyProfile createBeautyProfile(Member member) {
        return BeautyProfile.of(
                member,
                SkinType.DRY,
                SkinTone.WARM,
                Arrays.asList("여드름", "건조함"),
                false,
                null,
                Arrays.asList("스킨케어", "메이크업"),
                10000,
                50000,
                "추가 정보"
        );
    }

    public static BeautyProfileRequest createValidRequest() {
        return new BeautyProfileRequest(
                "DRY",
                "WARM",
                Arrays.asList("여드름", "건조함"),
                false,
                null,
                Arrays.asList("스킨케어", "메이크업"),
                10000,
                50000,
                "추가 정보"
        );
    }

    public static BeautyProfileRequest createUpdateRequest() {
        return new BeautyProfileRequest(
                "OILY",
                "COOL",
                Arrays.asList("모공", "과도한 유분"),
                true,
                Arrays.asList("방부제"),
                Arrays.asList("스킨케어", "메이크업", "헤어케어"),
                15000,
                80000,
                "업데이트된 추가 정보"
        );
    }

    private static void setMemberId(Member member, Long id) {
        try {
            Field idField = Member.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(member, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set member id for test", e);
        }
    }
}