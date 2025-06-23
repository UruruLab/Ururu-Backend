package com.ururulab.ururu.member.service;

import com.ururulab.ururu.global.common.entity.enumerated.SkinType;
import com.ururulab.ururu.member.domain.entity.BeautyProfile;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.repository.BeautyProfileRepository;
import com.ururulab.ururu.member.domain.repository.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BeautyProfileService {
    private final BeautyProfileRepository beautyProfileRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public BeautyProfile createBeautyProfile(Long memberId, SkinType skinType, List<String> concerns, List<String> allergies, List<String> interestCategories, String additionalInfo){
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "회원을 찾을 수 없습니다. ID: " + memberId));

        BeautyProfile beautyProfile = BeautyProfile.of(
                member, skinType, concerns, allergies, interestCategories, additionalInfo
        );

        BeautyProfile savedProfile = beautyProfileRepository.save(beautyProfile);
        log.info("BeautyProfile created for member ID: {}", memberId);

        return savedProfile;
    }

    public BeautyProfile getBeautyProfile(Long memberId) {
        return beautyProfileRepository.findByMemberId(memberId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "뷰티 프로필을 찾을 수 없습니다. Member ID: "+ memberId));
    }

    @Transactional
    public BeautyProfile updateBeautyProfile(
            Long memberId,
            SkinType skinType,
            List<String> concerns,
            List<String> allergies,
            List<String> interestCategories,
            String additionalInfo
    ) {
        BeautyProfile beautyProfile = beautyProfileRepository.findByMemberId(memberId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "뷰티 프로필을 찾을 수 없습니다. Member ID: " + memberId));

        beautyProfile.updateProfile(skinType, concerns, allergies, interestCategories, additionalInfo);

        BeautyProfile updatedProfile = beautyProfileRepository.save(beautyProfile);
        log.info("BeautyProfile updated for member ID: {}", memberId);

        return updatedProfile;
    }
}
