package com.ururulab.ururu.member.service;

import com.ururulab.ururu.global.domain.entity.enumerated.SkinTone;
import com.ururulab.ururu.global.domain.entity.enumerated.SkinType;
import com.ururulab.ururu.member.controller.dto.request.BeautyProfileRequest;
import com.ururulab.ururu.member.controller.dto.response.BeautyProfileCreateResponse;
import com.ururulab.ururu.member.controller.dto.response.BeautyProfileGetResponse;
import com.ururulab.ururu.member.controller.dto.response.BeautyProfileUpdateResponse;
import com.ururulab.ururu.member.domain.entity.BeautyProfile;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.repository.BeautyProfileRepository;
import com.ururulab.ururu.member.domain.repository.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BeautyProfileService {
    private final BeautyProfileRepository beautyProfileRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public BeautyProfileCreateResponse createBeautyProfile(Long memberId, BeautyProfileRequest request){
        if (beautyProfileRepository.existsByMemberId(memberId)) {
            throw new IllegalStateException("이미 뷰티 프로필이 존재합니다.");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "회원을 찾을 수 없습니다. ID: " + memberId));

        request.validateBusinessRules();
        SkinType skinType = parseSkinType(request.skinType());
        SkinTone skinTone = parseSkinTone(request.skinTone());

        BeautyProfile beautyProfile = BeautyProfile.of(
                member,
                skinType,
                skinTone,
                request.concerns(),
                request.hasAllergy(),
                request.allergies(),
                request.interestCategories(),
                request.minPrice(),
                request.maxPrice(),
                request.additionalInfo()
        );

        BeautyProfile savedProfile = beautyProfileRepository.save(beautyProfile);
        log.debug("BeautyProfile created for member ID: {}", memberId);

        return BeautyProfileCreateResponse.from(savedProfile);
    }

    @Transactional(readOnly = true)
    public BeautyProfileGetResponse getBeautyProfile(Long memberId) {
        BeautyProfile beautyProfile =  beautyProfileRepository.findByMemberId(memberId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "뷰티 프로필을 찾을 수 없습니다. Member ID: "+ memberId));

        return BeautyProfileGetResponse.from(beautyProfile);
    }

    @Transactional
    public BeautyProfileUpdateResponse updateBeautyProfile(Long memberId, BeautyProfileRequest request) {
        BeautyProfile beautyProfile = beautyProfileRepository.findByMemberId(memberId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "뷰티 프로필을 찾을 수 없습니다. Member ID: " + memberId));

        request.validateBusinessRules();
        SkinType skinType = parseSkinType(request.skinType());
        SkinTone skinTone = parseSkinTone(request.skinTone());

        beautyProfile.updateProfile(
                skinType,
                skinTone,
                request.concerns(),
                request.hasAllergy(),
                request.allergies(),
                request.interestCategories(),
                request.minPrice(),
                request.maxPrice(),
                request.additionalInfo()
        );

        BeautyProfile updatedProfile = beautyProfileRepository.save(beautyProfile);
        log.debug("BeautyProfile updated for member ID: {}", memberId);

        return BeautyProfileUpdateResponse.from(updatedProfile);
    }

    @Transactional
    public void deleteBeautyProfile(Long memberId) {
        if (!beautyProfileRepository.existsByMemberId(memberId)) {
            throw new EntityNotFoundException("뷰티 프로필을 찾을 수 없습니다. Member ID: " + memberId);
        }

        beautyProfileRepository.deleteByMemberId(memberId);
        log.debug("BeautyProfile deleted for member ID: {}", memberId);
    }

    private SkinType parseSkinType(final String skinTypeString) {
        if (skinTypeString == null) {
            return null;
        }
        try {
            return SkinType.from(skinTypeString);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("올바른 피부 타입 값이 아닙니다: " + skinTypeString, e);
        }
    }

    private SkinTone parseSkinTone(final String skinToneString) {
        if (skinToneString == null) {
            return null;
        }
        try {
            return SkinTone.from(skinToneString);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("올바른 피부 톤 값이 아닙니다: " + skinToneString, e);
        }
    }
}
