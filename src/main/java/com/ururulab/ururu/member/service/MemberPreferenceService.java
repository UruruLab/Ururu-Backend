package com.ururulab.ururu.member.service;

import com.ururulab.ururu.global.domain.entity.enumerated.EnumParser;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import com.ururulab.ururu.member.domain.entity.Member;
import com.ururulab.ururu.member.domain.entity.MemberPreference;
import com.ururulab.ururu.member.domain.entity.enumerated.PurchaseFrequency;
import com.ururulab.ururu.member.domain.repository.MemberPreferenceRepository;
import com.ururulab.ururu.member.domain.repository.MemberRepository;
import com.ururulab.ururu.member.dto.request.MemberPreferenceRequest;
import com.ururulab.ururu.member.dto.response.MemberPreferenceResponse;
import com.ururulab.ururu.seller.domain.repository.SellerRepository;
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
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_EXIST));

        if (request.sellerId() == null || request.sellerId() <= 0) {
            throw new BusinessException(ErrorCode.SELLER_NOT_FOUND);
        }

        validateSellerExists(request.sellerId());

        final boolean exists = memberPreferenceRepository.existsByMemberIdAndSellerId(memberId, request.sellerId());
        if (exists) {
            throw new BusinessException(ErrorCode.MEMBER_PREFERENCE_ALREADY_EXISTS);
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
            throw new BusinessException(ErrorCode.MEMBER_NOT_EXIST);
        }

        final List<MemberPreference> preferences = memberPreferenceRepository.findByMemberId(memberId);
        return preferences.stream()
                .map(MemberPreferenceResponse::from)
                .toList();
    }

    private void validateSellerExists(final Long sellerId) {
        if (!sellerRepository.existsByIdAndIsDeletedFalse(sellerId)) {
            throw new BusinessException(ErrorCode.SELLER_NOT_FOUND);
        }
    }

    private PurchaseFrequency parsePurchaseFrequency(final String purchaseFrequencyString) {
        if (purchaseFrequencyString == null) {
            throw new BusinessException(ErrorCode.PURCHASE_FREQUENCY_REQUIRED);
        }
        try {
            return EnumParser.fromString(PurchaseFrequency.class, purchaseFrequencyString, "PurchaseFrequency");
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_PURCHASE_FREQUENCY);
        }
    }
}
