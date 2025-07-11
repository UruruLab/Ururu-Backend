package com.ururulab.ururu.member.service;

import com.ururulab.ururu.global.domain.entity.enumerated.Gender;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.entity.MemberPreference;
import com.ururulab.ururu.member.domain.entity.enumerated.PurchaseFrequency;
import com.ururulab.ururu.member.domain.entity.enumerated.Role;
import com.ururulab.ururu.member.domain.entity.enumerated.SocialProvider;
import com.ururulab.ururu.member.dto.request.MemberPreferenceRequest;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

public class MemberPreferenceTestFixture {

    public static Member createMember(Long id) {
        Member member = Member.of(
                "testUser",
                "test@example.com",
                SocialProvider.GOOGLE,
                "social123",
                Gender.FEMALE,
                Instant.parse("1990-01-01T00:00:00Z"),
                "01012345678",
                "profile.jpg",
                Role.NORMAL
        );
        setMemberId(member, id);
        return member;
    }

    public static MemberPreferenceRequest createValidRequest(Long sellerId) {
        return new MemberPreferenceRequest(
                sellerId,
                4,
                50000,
                "MEDIUM"
        );
    }

    public static MemberPreferenceRequest createHighPreferenceRequest(Long sellerId) {
        return new MemberPreferenceRequest(
                sellerId,
                5,
                100000,
                "HIGH"
        );
    }

    public static MemberPreferenceRequest createLowPreferenceRequest(Long sellerId) {
        return new MemberPreferenceRequest(
                sellerId,
                1,
                10000,
                "LOW"
        );
    }

    public static MemberPreferenceRequest createInvalidSellerIdRequest(Long sellerId) {
        return new MemberPreferenceRequest(
                sellerId,  // null 또는 0
                4,
                50000,
                "MEDIUM"
        );
    }

    public static MemberPreferenceRequest createInvalidPurchaseFrequencyRequest(Long sellerId) {
        return new MemberPreferenceRequest(
                sellerId,
                4,
                50000,
                "INVALID_FREQUENCY"
        );
    }

    public static MemberPreference createMemberPreference(Member member, Long sellerId) {
        MemberPreference preference = MemberPreference.of(
                member,
                sellerId,
                4,
                50000,
                PurchaseFrequency.MEDIUM
        );
        setPreferenceId(preference, 1L);
        return preference;
    }

    public static MemberPreference createHighPreferenceMemberPreference(Member member, Long sellerId) {
        MemberPreference preference = MemberPreference.of(
                member,
                sellerId,
                5,
                100000,
                PurchaseFrequency.HIGH
        );
        setPreferenceId(preference, 1L);
        return preference;
    }

    public static List<MemberPreference> createMemberPreferenceList(Long memberId) {
        Member member = createMember(memberId);

        MemberPreference pref1 = MemberPreference.of(member, 100L, 4, 50000, PurchaseFrequency.MEDIUM);
        setPreferenceId(pref1, 1L);

        MemberPreference pref2 = MemberPreference.of(member, 101L, 5, 80000, PurchaseFrequency.HIGH);
        setPreferenceId(pref2, 2L);

        MemberPreference pref3 = MemberPreference.of(member, 102L, 3, 30000, PurchaseFrequency.LOW);
        setPreferenceId(pref3, 3L);

        return Arrays.asList(pref1, pref2, pref3);
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

    private static void setPreferenceId(MemberPreference preference, Long id) {
        try {
            Field idField = MemberPreference.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(preference, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set preference id for test", e);
        }
    }
}