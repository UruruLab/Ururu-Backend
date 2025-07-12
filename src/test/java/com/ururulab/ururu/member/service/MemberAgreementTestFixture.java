package com.ururulab.ururu.member.service;

import com.ururulab.ururu.global.domain.entity.enumerated.Gender;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.entity.MemberAgreement;
import com.ururulab.ururu.member.domain.entity.enumerated.AgreementType;
import com.ururulab.ururu.member.domain.entity.enumerated.Role;
import com.ururulab.ururu.member.domain.entity.enumerated.SocialProvider;
import com.ururulab.ururu.member.dto.request.MemberAgreementRequest;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MemberAgreementTestFixture {

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

    public static MemberAgreementRequest createValidAgreementRequest() {
        List<MemberAgreementRequest.AgreementItem> agreements = Arrays.asList(
                MemberAgreementRequest.AgreementItem.of(AgreementType.TERMS_OF_SERVICE, true),
                MemberAgreementRequest.AgreementItem.of(AgreementType.PRIVACY_POLICY, true),
                MemberAgreementRequest.AgreementItem.of(AgreementType.MARKETING, false),
                MemberAgreementRequest.AgreementItem.of(AgreementType.LOCATION, true)
        );
        return new MemberAgreementRequest(agreements);
    }

    public static MemberAgreementRequest createAllAgreedRequest() {
        List<MemberAgreementRequest.AgreementItem> agreements = Arrays.asList(
                MemberAgreementRequest.AgreementItem.of(AgreementType.TERMS_OF_SERVICE, true),
                MemberAgreementRequest.AgreementItem.of(AgreementType.PRIVACY_POLICY, true),
                MemberAgreementRequest.AgreementItem.of(AgreementType.MARKETING, true),
                MemberAgreementRequest.AgreementItem.of(AgreementType.LOCATION, true)
        );
        return new MemberAgreementRequest(agreements);
    }

    public static MemberAgreementRequest createRequiredOnlyRequest() {
        List<MemberAgreementRequest.AgreementItem> agreements = Arrays.asList(
                MemberAgreementRequest.AgreementItem.of(AgreementType.TERMS_OF_SERVICE, true),
                MemberAgreementRequest.AgreementItem.of(AgreementType.PRIVACY_POLICY, true)
        );
        return new MemberAgreementRequest(agreements);
    }


    public static List<MemberAgreement> createAllAgreedMemberAgreements(Member member) {
        List<MemberAgreement> agreements = new ArrayList<>();
        agreements.add(MemberAgreement.of(member, AgreementType.TERMS_OF_SERVICE, true));
        agreements.add(MemberAgreement.of(member, AgreementType.PRIVACY_POLICY, true));
        agreements.add(MemberAgreement.of(member, AgreementType.MARKETING, true));
        agreements.add(MemberAgreement.of(member, AgreementType.LOCATION, true));

        // ID 설정
        for (int i = 0; i < agreements.size(); i++) {
            setAgreementId(agreements.get(i), (long) (i + 1));
        }

        return agreements;
    }

    public static List<MemberAgreement> createRequiredOnlyMemberAgreements(Member member) {
        List<MemberAgreement> agreements = new ArrayList<>();
        agreements.add(MemberAgreement.of(member, AgreementType.TERMS_OF_SERVICE, true));
        agreements.add(MemberAgreement.of(member, AgreementType.PRIVACY_POLICY, true));

        // ID 설정
        for (int i = 0; i < agreements.size(); i++) {
            setAgreementId(agreements.get(i), (long) (i + 1));
        }

        return agreements;
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

    private static void setAgreementId(MemberAgreement agreement, Long id) {
        try {
            Field idField = MemberAgreement.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(agreement, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set agreement id for test", e);
        }
    }
}