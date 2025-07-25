package com.ururulab.ururu.ai.service;

import com.ururulab.ururu.ai.config.AiRecommendationProperties;
import com.ururulab.ururu.ai.dto.GroupBuyRecommendationRequest;
import com.ururulab.ururu.member.domain.entity.BeautyProfile;
import com.ururulab.ururu.member.domain.repository.BeautyProfileRepository;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class BeautyProfileConversionService {

    private final BeautyProfileRepository beautyProfileRepository;
    private final AiRecommendationProperties aiProperties;

    public GroupBuyRecommendationRequest convertToRecommendationRequest(final Long memberId, final Integer topK) {
        final BeautyProfile beautyProfile = beautyProfileRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BEAUTY_PROFILE_INCOMPLETE));

        log.info("BeautyProfile 기반 AI 추천 요청 변환 - 회원ID: {}, 피부타입: {}",
                memberId, beautyProfile.getSkinType());

        final GroupBuyRecommendationRequest.BeautyProfile aiBeautyProfile =
                new GroupBuyRecommendationRequest.BeautyProfile(
                        beautyProfile.getSkinType() != null ? beautyProfile.getSkinType().toString() : "OILY",
                        beautyProfile.getSkinTone() != null ? beautyProfile.getSkinTone().toString() : "WARM",
                        beautyProfile.getConcerns(),
                        beautyProfile.getHasAllergy(),
                        beautyProfile.getAllergies(),
                        beautyProfile.getInterestCategories()
                );

        return new GroupBuyRecommendationRequest(
                aiBeautyProfile,
                topK != null ? topK : aiProperties.getDefaultTopK(),
                beautyProfile.getMinPrice(),
                beautyProfile.getMaxPrice(),
                beautyProfile.getAdditionalInfo(),
                beautyProfile.getInterestCategories(),
                aiProperties.getDefaultMinSimilarity(),
                aiProperties.getDefaultUsePriceFilter()
        );
    }
}
