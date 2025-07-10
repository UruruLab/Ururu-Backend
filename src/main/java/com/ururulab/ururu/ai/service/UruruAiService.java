package com.ururulab.ururu.ai.service;

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

    @Qualifier("aiServiceRestClient")
    private final RestClient aiServiceRestClient;

    public List<RecommendedGroupBuy> getRecommendations(final GroupBuyRecommendationRequest request) {
        try {
            log.info("AI 추천 서비스 호출 시작 - 회원ID: {}, 피부타입: {}",
                    request.memberId(), request.skinType());

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
                log.error("AI 서비스 타임아웃 발생 - 회원ID: {}", request.memberId(), e);
                throw new BusinessException(ErrorCode.AI_SERVICE_TIMEOUT);
            }
            log.error("AI 서비스 연결 실패 - 회원ID: {}", request.memberId(), e);
            throw new BusinessException(ErrorCode.AI_SERVICE_CONNECTION_FAILED);
        } catch (final Exception e) {
            log.error("AI 추천 서비스 호출 중 예외 발생 - 회원ID: {}", request.memberId(), e);
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }
    }

    private Map<String, Object> buildAiRequest(final GroupBuyRecommendationRequest request) {
        final String userDiagnosis = buildUserDiagnosis(request);

        return Map.of(
                "user_diagnosis", userDiagnosis,
                "top_k", request.topK(),
                "max_price", extractMaxPrice(request.priceRange())
        );
    }

    private String buildUserDiagnosis(final GroupBuyRecommendationRequest request) {
        final StringBuilder diagnosis = new StringBuilder();

        diagnosis.append(request.ageGroup() != null ? request.ageGroup() + " " : "")
                .append(request.skinType()).append(" 피부, ");

        if (request.skinConcerns() != null && !request.skinConcerns().isEmpty()) {
            diagnosis.append(String.join(", ", request.skinConcerns())).append("으로 고민이 있어요. ");
        }

        if (request.preferredCategories() != null && !request.preferredCategories().isEmpty()) {
            diagnosis.append(String.join(", ", request.preferredCategories())).append(" 제품을 선호해요.");
        }

        return diagnosis.toString().trim();
    }

    private Integer extractMaxPrice(final String priceRange) {
        if (priceRange == null || priceRange.isEmpty()) {
            return 100000;
        }

        return switch (priceRange) {
            case "10000원 미만" -> 10000;
            case "10000-30000원" -> 30000;
            case "30000-50000원" -> 50000;
            case "50000-100000원" -> 100000;
            case "100000원 이상" -> 200000;
            default -> 100000;
        };
    }
}
