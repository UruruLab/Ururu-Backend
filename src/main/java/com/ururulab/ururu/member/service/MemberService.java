package com.ururulab.ururu.member.service;

import com.ururulab.ururu.auth.dto.info.SocialMemberInfo;
import com.ururulab.ururu.global.common.entity.enumerated.Gender;
import com.ururulab.ururu.member.domain.dto.request.MemberRequest;
import com.ururulab.ururu.member.domain.dto.response.*;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.entity.enumerated.Role;
import com.ururulab.ururu.member.domain.repository.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;

    /**
     * 소셜 회원 정보로 새 회원을 생성하거나 기존 회원을 조회합니다.
     *
     * @param socialMemberInfo 소셜 로그인에서 받은 회원 정보
     * @return 생성되거나 조회된 회원
     */
    @Transactional
    public Member findOrCreateMember(final SocialMemberInfo socialMemberInfo) {
        return memberRepository.findBySocialProviderAndSocialId(
                socialMemberInfo.provider(),
                socialMemberInfo.socialId()
        ).orElseGet(() -> createNewMember(socialMemberInfo));
    }

    /**
     * 회원 생성 요청으로 새 회원을 생성합니다.
     *
     * @param request 회원 생성 요청 정보
     * @return 생성된 회원
     */
    @Transactional
    public Member createMember(final MemberRequest request) {
        validateMemberCreation(request);

        final Member member = Member.of(
                request.nickname(),
                request.email(),
                request.socialProvider(),
                request.socialId(),
                parseGender(request.gender()),
                request.birth(),
                request.phone(),
                request.profileImage(),
                Role.NORMAL
        );

        final Member savedMember = memberRepository.save(member);
        log.info("New member created with ID: {}", savedMember.getId());

        return savedMember;
    }

    public MemberResponse checkEmail(final String email) {
        final boolean isAvailable = memberRepository.isEmailAvailable(email);
        return MemberResponse.ofAvailabilityCheck(isAvailable);
    }

    public MemberResponse checkNickname(final String nickname) {
        final boolean isAvailable = memberRepository.isNicknameAvailable(nickname);
        return MemberResponse.ofAvailabilityCheck(isAvailable);
    }

    public GetMemberResponse getMemberProfile(final Long memberId) {
        final Member member = findActiveMemberById(memberId);
        return GetMemberResponse.from(member);
    }

    public GetMyProfileResponse getMyProfile(final Long memberId) {
        final Member member = findActiveMemberById(memberId);
        return GetMyProfileResponse.from(member);
    }

    @Transactional
    public UpdateMemberResponse updateMemberProfile(final Long memberId, final MemberRequest request) {
        final Member updatedMember = updateProfile(memberId, request);
        return UpdateMemberResponse.from(updatedMember);
    }

    @Transactional
    public UpdateMyProfileResponse updateMyProfile(final Long memberId, final MemberRequest request) {
        final Member updatedMember = updateProfile(memberId, request);
        return UpdateMyProfileResponse.from(updatedMember);
    }

    private Member updateProfile(final Long memberId, final MemberRequest request) {
        final Member member = findActiveMemberById(memberId);

        if (request.nickname() != null && !request.nickname().equals(member.getNickname())) {
            if (!memberRepository.isNicknameAvailable(request.nickname())) {
                throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
            }
        }

        updateMemberFields(member, request);

        final Member updatedMember = memberRepository.save(member);
        log.info("Member profile updated for ID: {}", memberId);

        return updatedMember;
    }


    private Member createNewMember(final SocialMemberInfo socialMemberInfo) {
        final Member member = Member.of(
                socialMemberInfo.nickname(),
                socialMemberInfo.email(),
                socialMemberInfo.provider(),
                socialMemberInfo.socialId(),
                null,
                null,
                null,
                socialMemberInfo.profileImage(),
                Role.NORMAL
        );

        final Member savedMember = memberRepository.save(member);
        log.info("New social member created with ID: {} for provider: {}",
                savedMember.getId(), socialMemberInfo.provider());

        return savedMember;
    }

    private void validateMemberCreation(final MemberRequest request) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        if (memberRepository.existsByNickname(request.nickname())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        if (memberRepository.existsBySocialProviderAndSocialId(
                request.socialProvider(), request.socialId())) {
            throw new IllegalArgumentException("이미 등록된 소셜 계정입니다.");
        }
    }

    private void updateMemberFields(final Member member, final MemberRequest request) {
        if (request.nickname() != null) { member.updateNickname(request.nickname());}

        if (request.gender() != null) { member.updateGender(parseGender(request.gender()));}

        if (request.birth() != null) { member.updateBirth(request.birth());}

        if (request.phone() != null) { member.updatePhone(request.phone());}
    }

    private Member findActiveMemberById(final Long memberId) {
        return memberRepository.findById(memberId)
                .filter(member -> !member.isDeleted())
                .orElseThrow(() -> new EntityNotFoundException(
                        "회원을 찾을 수 없습니다. ID: " + memberId));
    }

    private Gender parseGender(final String genderString) {
        if (genderString == null) {
            return null;
        }
        try {
            return Gender.from(genderString);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("올바른 성별 값이 아닙니다: " + genderString, e);
        }
    }
}