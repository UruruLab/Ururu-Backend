package com.ururulab.ururu.ai.service;

import com.ururulab.ururu.ai.config.AiRecommendationProperties;
import com.ururulab.ururu.ai.dto.GroupBuyRecommendationRequest;
import com.ururulab.ururu.ai.dto.GroupBuyRecommendationResponse.RecommendedGroupBuy;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UruruAiService {

    private static final String HEALTH_STATUS_NORMAL = "정상";
    private static final String HEALTH_STATUS_ERROR_PREFIX = "정상 오류: ";

    @Qualifier("aiServiceRestClient")
    private final RestClient aiServiceRestClient;
    private final AiRecommendationProperties aiProperties;

    public List<RecommendedGroupBuy> getRecommendations(final Long memberId, final GroupBuyRecommendationRequest request) {
        try {
            log.info("AI 추천 서비스 호출 시작 - 회원ID: {}, 피부타입: {}",
                    memberId, request.beautyProfile().skinType());

            final Map<String, Object> aiRequest = buildAiRequest(request);

            final ResponseEntity<List<RecommendedGroupBuy>> response = aiServiceRestClient
                    .post()
                    .uri("/api/recommendations/")
                    .body(aiRequest)
                    .retrieve()
                    .toEntity(new ParameterizedTypeReference<List<RecommendedGroupBuy>>() {
                    });

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.error("AI 서비스 응답 오류 - Status: {}", response.getStatusCode());
                throw new BusinessException(ErrorCode.AI_RECOMMENDATION_FAILED);
            }

            final List<RecommendedGroupBuy> recommendations = response.getBody();
            log.info("AI 추천 완료 - 추천 상품 수: {}", recommendations.size());

            return recommendations;

        } catch (final ResourceAccessException e) {
            if (e.getCause() instanceof SocketTimeoutException) {
                log.error("AI 서비스 타임아웃 발생 - 회원ID: {}", memberId, e);
                throw new BusinessException(ErrorCode.AI_SERVICE_TIMEOUT);
            }
            log.error("AI 서비스 연결 실패 - 회원ID: {}", memberId, e);
            throw new BusinessException(ErrorCode.AI_SERVICE_CONNECTION_FAILED);
        } catch (final Exception e) {
            log.error("AI 추천 서비스 호출 중 예외 발생 - 회원ID: {}", memberId, e);
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }
    }

    private Map<String, Object> buildAiRequest(final GroupBuyRecommendationRequest request) {
        return Map.of(
                "beauty_profile", Map.of(
                        "skin_type", request.beautyProfile().skinType(),
                        "skin_tone", request.beautyProfile().skinTone() != null ? request.beautyProfile().skinTone() : "",
                        "concerns", request.beautyProfile().concerns() != null ? request.beautyProfile().concerns() : List.of(),
                        "has_allergy", request.beautyProfile().hasAllergy() != null ? request.beautyProfile().hasAllergy() : false,
                        "allergies", request.beautyProfile().allergies() != null ? request.beautyProfile().allergies() : List.of(),
                        "interest_categories", request.beautyProfile().interestCategories() != null ? request.beautyProfile().interestCategories() : List.of()
                ),
                "top_k", request.topK() != null ? request.topK() : aiProperties.getDefaultTopK(),
                "include_categories", request.interestCategories() != null ? request.interestCategories() : List.of(),
                "min_similarity", request.minSimilarity() != null ? request.minSimilarity() : aiProperties.getDefaultMinSimilarity(),
                "use_price_filter", request.usePriceFilter() != null ? request.usePriceFilter() : aiProperties.isDefaultUsePriceFilter(),
                "min_price", request.minPrice() != null ? request.minPrice() : 10,
                "max_price", request.maxPrice() != null ? request.maxPrice() : 1000000,
                "additional_info", request.additionalInfo() != null ? request.additionalInfo() : ""
        );
    }

    public String checkHealth() {
        try {
            log.debug("AI 서비스 헬스 체크 시작");

            final ResponseEntity<String> response = aiServiceRestClient
                    .get()
                    .uri("/health")
                    .retrieve()
                    .toEntity(String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.debug("AI 서비스 헬스 체크 성공");
                return HEALTH_STATUS_NORMAL;
            } else {
                log.warn("AI 서비스 헬스 체크 실패 - Status: {}", response.getStatusCode());
                return HEALTH_STATUS_ERROR_PREFIX + response.getStatusCode();
            }
        } catch (final ResourceAccessException e) {
            log.error("AI 서비스 연결 실패", e);
            throw new BusinessException(ErrorCode.AI_SERVICE_CONNECTION_FAILED);
        } catch (final Exception e) {
            log.error("AI 서비스 헬스 체크 중 예외 발생", e);
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }
    }
}