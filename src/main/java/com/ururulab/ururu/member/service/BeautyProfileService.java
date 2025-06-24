package com.ururulab.ururu.member.service;

import com.ururulab.ururu.member.domain.dto.request.BeautyProfileRequest;
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
    public BeautyProfile createBeautyProfile(Long memberId, BeautyProfileRequest request){
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "회원을 찾을 수 없습니다. ID: " + memberId));

        if (request.hasAllergy() && (request.allergies() == null || request.allergies().isEmpty())) {
            throw new IllegalArgumentException("알러지가 있다고 선택하셨습니다. 알러지 목록을 입력해주세요.");
        }

        if (!request.hasAllergy() && request.allergies() != null && !request.allergies().isEmpty()) {
            throw new IllegalArgumentException("알러지가 없다고 선택하셨습니다. 알러지 목록을 비워주세요.");
        }

        if (request.minPrice() > request.maxPrice()) {
            throw new IllegalArgumentException("최소 가격은 최대 가격보다 작거나 같아야 합니다.");
        }

        BeautyProfile beautyProfile = BeautyProfile.of(
                member,
                request.skinType(),
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

        return savedProfile;
    }

    public BeautyProfile getBeautyProfile(Long memberId) {
        return beautyProfileRepository.findByMemberId(memberId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "뷰티 프로필을 찾을 수 없습니다. Member ID: "+ memberId));
    }

    @Transactional
    public BeautyProfile updateBeautyProfile(Long memberId, BeautyProfileRequest request) {
        BeautyProfile beautyProfile = beautyProfileRepository.findByMemberId(memberId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "뷰티 프로필을 찾을 수 없습니다. Member ID: " + memberId));

        beautyProfile.updateProfile(
                request.skinType(),
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

        return updatedProfile;
    }
}
