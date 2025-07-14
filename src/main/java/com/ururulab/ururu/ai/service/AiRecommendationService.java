package com.ururulab.ururu.ai.service;

import com.ururulab.ururu.ai.dto.GroupBuyRecommendationRequest;
import com.ururulab.ururu.ai.dto.GroupBuyRecommendationResponse.RecommendedGroupBuy;
import com.ururulab.ururu.global.client.AiServiceClient;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.Builder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * AI 추천 서비스와의 통합을 담당하는 서비스 계층.
 * 
 * 책임: AI 서비스와의 통신 조율 및 데이터 변환 조율
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiRecommendationService {

    private final AiServiceClient aiServiceClient;
    private final AiRequestMappingService requestMappingService;
    private final AiResponseMappingService responseMappingService;

    /**
     * AI 서비스 상태 확인.
     *
     * @return 상태 확인 결과 DTO
     */
    public HealthCheckResponse checkHealth() {
        try {
            log.debug("AI 서비스 헬스체크 시작");
            
            final long startTime = System.currentTimeMillis();
            final boolean isHealthy = aiServiceClient.checkHealthStatus();
            final long responseTime = System.currentTimeMillis() - startTime;
            
            log.debug("AI 서비스 헬스체크 완료 - 응답시간: {}ms", responseTime);
            
            return HealthCheckResponse.builder()
                    .healthy(isHealthy)
                    .responseTimeMs(responseTime)
                    .message(isHealthy ? "AI 서비스 정상" : "AI 서비스 응답 오류")
                    .build();
            
        } catch (final BusinessException e) {
            log.error("AI 서비스 헬스체크 실패", e);
            return HealthCheckResponse.builder()
                    .healthy(false)
                    .responseTimeMs(0L)
                    .message("AI 서비스 연결 실패: " + e.getMessage())
                    .build();
        }
    }

    @Builder
    public record HealthCheckResponse(
            boolean healthy,
            long responseTimeMs,
            String message
    ) {}

    /**
     * AI 서비스에서 추천 결과 조회.
     *
     * @param memberId 회원 ID
     * @param request 추천 요청
     * @return 추천 상품 리스트
     * @throws BusinessException AI 서비스 통신 실패 시
     */
    public List<RecommendedGroupBuy> getRecommendations(final Long memberId, final GroupBuyRecommendationRequest request) {
        log.info("AI 추천 요청 시작 - 회원ID: {}, 피부타입: {}", 
                memberId, request.beautyProfile().skinType());

        try {
            // 1. Spring Boot 요청을 AI 서비스 형식으로 변환
            final Map<String, Object> aiRequest = requestMappingService.mapToAiRequest(request);
            
            // 2. AI 서비스 호출
            final Map<String, Object> aiResponse = aiServiceClient.requestRecommendations(aiRequest);
            
            // 3. AI 응답을 Spring Boot 형식으로 변환
            final List<RecommendedGroupBuy> recommendations = responseMappingService.mapToRecommendedGroupBuys(aiResponse);
            
            if (recommendations.isEmpty()) {
                throw new BusinessException(ErrorCode.AI_NO_RECOMMENDATIONS_FOUND);
            }

            log.info("AI 추천 완료 - 회원ID: {}, 추천 수: {}", memberId, recommendations.size());
            return recommendations;

        } catch (final BusinessException e) {
            // 이미 적절한 예외로 변환된 경우 재전파
            throw e;
        } catch (final Exception e) {
            log.error("AI 추천 처리 중 예상치 못한 오류 - 회원ID: {}", memberId, e);
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }
    }
}
