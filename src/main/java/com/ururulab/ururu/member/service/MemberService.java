package com.ururulab.ururu.member.service;

import com.ururulab.ururu.global.auth.dto.info.SocialMemberInfo;
import com.ururulab.ururu.global.common.entity.enumerated.Gender;
import com.ururulab.ururu.member.domain.dto.request.CreateMemberRequest;
import com.ururulab.ururu.member.domain.dto.response.EmailCheckResponse;
import com.ururulab.ururu.member.domain.dto.response.MemberProfileResponse;
import com.ururulab.ururu.member.domain.dto.response.NicknameCheckResponse;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.entity.enumerated.Role;
import com.ururulab.ururu.member.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private static final DateTimeFormatter BIRTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

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
    public Member createMember(final CreateMemberRequest request) {
        validateMemberCreation(request);

        final Member member = Member.of(
                request.nickname(),
                request.email(),
                request.socialProvider(),
                request.socialId(),
                parseGender(request.gender()),
                parseBirthDate(request.birth()),
                request.phone(),
                request.profileImage(),
                Role.NORMAL
        );

        final Member savedMember = memberRepository.save(member);
        log.info("New member created with ID: {}", savedMember.getId());

        return savedMember;
    }

    public EmailCheckResponse checkEmail(final String email) {
        final boolean isAvailable = memberRepository.isEmailAvailable(email);
        return EmailCheckResponse.of(isAvailable);
    }

    public NicknameCheckResponse checkNickname(final String nickname) {
        final boolean isAvailable = memberRepository.isNicknameAvailable(nickname);
        return NicknameCheckResponse.of(isAvailable);
    }

    public MemberProfileResponse getMemberProfile(final Long memberId) {
        final Member member = findActiveMemberById(memberId);
        return MemberProfileResponse.from(member);
    }

    private Member createNewMember(final SocialMemberInfo socialMemberInfo) {
        final Member member = Member.of(
                socialMemberInfo.nickname(),
                socialMemberInfo.email(),
                socialMemberInfo.provider(),
                socialMemberInfo.socialId(),
                null, // gender
                null, // birth
                null, // phone
                socialMemberInfo.profileImage(),
                Role.NORMAL
        );

        final Member savedMember = memberRepository.save(member);
        log.info("New social member created with ID: {} for provider: {}",
                savedMember.getId(), socialMemberInfo.provider());

        return savedMember;
    }

    private void validateMemberCreation(final CreateMemberRequest request) {
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

    private Gender parseGender(final String genderString) {
        if (genderString == null) {
            return null;
        }
        return Gender.from(genderString);
    }

    private LocalDateTime parseBirthDate(final String birthString) {
        if (birthString == null) {
            return null;
        }

        try {
            return LocalDateTime.parse(birthString + "T00:00:00");
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("올바른 생년월일 형식이 아닙니다: " + birthString, e);
        }
    }
}