package com.ururulab.ururu.member.service;

import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.entity.MemberPreference;
import com.ururulab.ururu.member.domain.entity.enumerated.PurchaseFrequency;
import com.ururulab.ururu.member.domain.repository.MemberPreferenceRepository;
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
public class MemberPreferenceService {
    private final MemberPreferenceRepository memberPreferenceRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public MemberPreference createPreference(
            Long memberId,
            Long sellerId,
            int preferenceLevel,
            int monthlyBudget,
            String preferredPriceRange,
            PurchaseFrequency purchaseFrequency
    ) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "회원을 찾을 수 없습니다. ID: " + memberId));

        if (sellerId == null || sellerId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 판매자 ID입니다. ID: " +sellerId);
        }

        if (preferenceLevel < 1 || preferenceLevel > 5) {
            throw new IllegalArgumentException("선호도 레벨은 1-5사이의 값이어야 합니다. 입력값: " +preferenceLevel);
        }

        if (monthlyBudget < 0) {
            throw new IllegalArgumentException("월 예산은 0 이상이어야 합니다. 입력값: " +monthlyBudget);
        }

        MemberPreference preference = MemberPreference.of(
                member, sellerId, preferenceLevel, monthlyBudget,
                preferredPriceRange, purchaseFrequency
        );

        MemberPreference savedPreference = memberPreferenceRepository.save(preference);
        log.info("Member preference created for member ID: {}, seller ID: {}", memberId, sellerId);

        return savedPreference;
    }

    public List<MemberPreference> getMemberPreferences(Long memberId) {
        return memberPreferenceRepository.findByMemberId(memberId);
    }
}
