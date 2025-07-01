package com.ururulab.ururu.member.service;

import com.ururulab.ururu.global.domain.entity.enumerated.EnumParser;
import com.ururulab.ururu.member.controller.dto.request.MemberPreferenceRequest;
import com.ururulab.ururu.member.controller.dto.response.MemberPreferenceResponse;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.entity.MemberPreference;
import com.ururulab.ururu.member.domain.entity.enumerated.PurchaseFrequency;
import com.ururulab.ururu.member.domain.repository.MemberPreferenceRepository;
import com.ururulab.ururu.member.domain.repository.MemberRepository;
import com.ururulab.ururu.seller.domain.repository.SellerRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberPreferenceService {
    private final MemberPreferenceRepository memberPreferenceRepository;
    private final MemberRepository memberRepository;
    private final SellerRepository sellerRepository;

    @Transactional
    public MemberPreferenceResponse createPreference(Long memberId, final MemberPreferenceRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "회원을 찾을 수 없습니다. ID: " + memberId));

        if (request.sellerId() == null || request.sellerId() <= 0) {
            throw new IllegalArgumentException("유효하지 않은 판매자 ID입니다. ID: " +request.sellerId());
        }

        validateSellerExists(request.sellerId());

        final boolean exists = memberPreferenceRepository.existsByMemberIdAndSellerId(memberId, request.sellerId());
        if (exists) {
            throw new IllegalStateException("해당 판매자에 대한 선호도가 이미 존재합니다.");
        }

        final PurchaseFrequency purchaseFrequency = parsePurchaseFrequency(request.purchaseFrequency());

        MemberPreference preference = MemberPreference.of(
                member,
                request.sellerId(),
                request.preferenceLevel(),
                request.monthlyBudget(),
                purchaseFrequency
        );

        MemberPreference savedPreference = memberPreferenceRepository.save(preference);
        log.debug("Member preference created for member ID: {}, seller ID: {}", memberId, request.sellerId());

        return MemberPreferenceResponse.from(savedPreference);
    }

    @Transactional(readOnly = true)
    public List<MemberPreferenceResponse> getMemberPreferences(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new EntityNotFoundException("회원을 찾을 수 없습니다. ID: " + memberId);
        }

        final List<MemberPreference> preferences = memberPreferenceRepository.findByMemberId(memberId);
        return preferences.stream()
                .map(MemberPreferenceResponse::from)
                .toList();
    }

    private void validateSellerExists(final Long sellerId) {
        if (!sellerRepository.existsByIdAndIsDeletedFalse(sellerId)) {
            throw new EntityNotFoundException("판매자를 찾을 수 없습니다. ID :" + sellerId);
        }
    }

    private PurchaseFrequency parsePurchaseFrequency(final String purchaseFrequencyString) {
        if (purchaseFrequencyString == null) {
            throw new IllegalArgumentException("구매 빈도는 필수입니다.");
        }
        try {
            return EnumParser.fromString(PurchaseFrequency.class, purchaseFrequencyString, "PurchaseFrequency");
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("올바른 구매 빈도 값이 아닙니다: " + purchaseFrequencyString, e);
        }
    }
}
