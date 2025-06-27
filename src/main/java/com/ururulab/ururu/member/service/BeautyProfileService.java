package com.ururulab.ururu.member.service;

import com.ururulab.ururu.global.common.entity.enumerated.SkinType;
import com.ururulab.ururu.member.domain.dto.request.BeautyProfileRequest;
import com.ururulab.ururu.member.domain.dto.response.CreateBeautyProfileResponse;
import com.ururulab.ururu.member.domain.dto.response.GetBeautyProfileResponse;
import com.ururulab.ururu.member.domain.dto.response.UpdateBeautyProfileResponse;
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
@Transactional(readOnly = true)
public class BeautyProfileService {
    private final BeautyProfileRepository beautyProfileRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public CreateBeautyProfileResponse createBeautyProfile(Long memberId, BeautyProfileRequest request){
        if (beautyProfileRepository.existsByMemberId(memberId)) {
            throw new IllegalStateException("이미 뷰티 프로필이 존재합니다.");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "회원을 찾을 수 없습니다. ID: " + memberId));

        request.validateBusinessRules();
        SkinType skinType = parseSkinType(request.skinType());

        BeautyProfile beautyProfile = BeautyProfile.of(
                member,
                skinType,
                request.concerns(),
                request.hasAllergy(),
                request.allergies(),
                request.interestCategories(),
                request.minPrice(),
                request.maxPrice(),
                request.additionalInfo()
        );

        BeautyProfile savedProfile = beautyProfileRepository.save(beautyProfile);
        log.info("BeautyProfile created for member ID: {}", memberId);

        return CreateBeautyProfileResponse.from(savedProfile);
    }

    public GetBeautyProfileResponse getBeautyProfile(Long memberId) {
        BeautyProfile beautyProfile =  beautyProfileRepository.findByMemberId(memberId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "뷰티 프로필을 찾을 수 없습니다. Member ID: "+ memberId));

        return GetBeautyProfileResponse.from(beautyProfile);
    }

    @Transactional
    public UpdateBeautyProfileResponse updateBeautyProfile(Long memberId, BeautyProfileRequest request) {
        BeautyProfile beautyProfile = beautyProfileRepository.findByMemberId(memberId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "뷰티 프로필을 찾을 수 없습니다. Member ID: " + memberId));

        request.validateBusinessRules();
        SkinType skinType = parseSkinType(request.skinType());

        beautyProfile.updateProfile(
                skinType,
                request.concerns(),
                request.hasAllergy(),
                request.allergies(),
                request.interestCategories(),
                request.minPrice(),
                request.maxPrice(),
                request.additionalInfo()
        );

        BeautyProfile updatedProfile = beautyProfileRepository.save(beautyProfile);
        log.info("BeautyProfile updated for member ID: {}", memberId);

        return UpdateBeautyProfileResponse.from(updatedProfile);
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
}
