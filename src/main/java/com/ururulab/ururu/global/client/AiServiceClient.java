package com.ururulab.ururu.global.client;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * AI 서비스와의 HTTP 통신을 담당하는 클라이언트.
 *
 * <p>Ururu-AI FastAPI 서버의 실제 엔드포인트와 연동하여 상품 추천, 헬스체크 등의 기능을 제공합니다.</p>
 */
@Slf4j
@Component
public class AiServiceClient {

    private final RestClient aiServiceRestClient;

    @Value("${ai.service.retry-count:3}")
    private int retryCount;

    @Value("${ai.service.max-recommendations:50}")
    private int maxRecommendations;

    public AiServiceClient(@Qualifier("aiServiceRestClient") final RestClient aiServiceRestClient) {
        this.aiServiceRestClient = aiServiceRestClient;
    }

    /**
     * AI 서비스 기본 헬스체크.
     *
     * @return 헬스체크 성공 여부
     */
    public boolean checkHealthStatus() {
        try {
            final var response = aiServiceRestClient
                    .get()
                    .uri("/health")
                    .retrieve()
                    .toEntity(Map.class);

            return response.getStatusCode() == HttpStatus.OK;

        } catch (final RestClientException e) {
            log.warn("AI 서비스 헬스체크 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * AI 추천 서비스 헬스체크.
     *
     * @return 헬스체크 결과 정보
     * @throws BusinessException AI 서비스 헬스체크 실패 시
     */
    public Map<String, Object> checkRecommendationHealth() {
        try {
            final var response = aiServiceRestClient
                    .get()
                    .uri("/api/recommendations/health")
                    .retrieve()
                    .toEntity(Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                final var body = response.getBody();
                if (body == null) {
                    throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
                }
                return body;
            }
            return Map.of("status", "unhealthy", "error", "Non-200 response");

        } catch (final RestClientException e) {
            log.warn("AI 추천 서비스 헬스체크 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.AI_SERVICE_HEALTH_CHECK_FAILED);
        }
    }

    /**
     * Spring Boot 연결 상태 확인.
     *
     * @return 연결 상태 정보
     * @throws BusinessException 연결 실패 시
     */
    public Map<String, Object> checkSpringBootConnection() {
        try {
            final var response = aiServiceRestClient
                    .get()
                    .uri("/api/recommendations/spring-health")
                    .retrieve()
                    .toEntity(Map.class);

            final var body = response.getBody();
            if (body == null) {
                throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
            }
            return body;

        } catch (final RestClientException e) {
            log.error("Spring Boot 연결 상태 확인 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.AI_SERVICE_CONNECTION_FAILED);
        }
    }

    /**
     * AI 추천 서비스에 추천 요청.
     *
     * @param requestBody AI 서비스 요청 본문
     * @return AI 서비스 원본 응답
     * @throws BusinessException AI 서비스 통신 실패 시
     */
    public Map<String, Object> requestRecommendations(final Map<String, Object> requestBody) {
        try {
            log.debug("AI 추천 서비스 호출 시작");

            final var response = aiServiceRestClient
                    .post()
                    .uri("/api/recommendations/")
                    .body(requestBody)
                    .retrieve()
                    .toEntity(Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.debug("AI 추천 서비스 호출 성공");
                return response.getBody();
            }

            log.error("AI 서비스 응답 오류 - Status: {}", response.getStatusCode());
            throw new BusinessException(ErrorCode.AI_RECOMMENDATION_FAILED);

        } catch (final RestClientException e) {
            if (isTimeoutException(e)) {
                log.error("AI 서비스 타임아웃 발생", e);
                throw new BusinessException(ErrorCode.AI_SERVICE_TIMEOUT);
            }
            if (isConnectionRefused(e)) {
                log.error("AI 서비스 연결 실패", e);
                throw new BusinessException(ErrorCode.AI_SERVICE_CONNECTION_FAILED);
            }
            log.error("AI 추천 서비스 호출 중 예외 발생", e);
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }
    }

    /**
     * Spring Boot 알림 테스트.
     *
     * @param memberId 회원 ID
     * @return 테스트 결과
     * @throws BusinessException 알림 테스트 실패 시
     */
    @Retryable(
            retryFor = {RestClientException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 500)
    )
    public Map<String, Object> testSpringNotification(final Long memberId) {
        try {
            final var response = aiServiceRestClient
                    .post()
                    .uri("/api/recommendations/test-spring-notify")
                    .body(Map.of("member_id", memberId))
                    .retrieve()
                    .toEntity(Map.class);

            log.info("Spring Boot 알림 테스트 완료 - memberId: {}", memberId);
            final var body = response.getBody();
            if (body == null) {
                throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
            }
            return body;

        } catch (final RestClientException e) {
            log.error("Spring Boot 알림 테스트 실패 - memberId: {}", memberId, e);
            throw new BusinessException(ErrorCode.AI_SERVICE_CONNECTION_FAILED);
        }
    }

    private void validateRecommendationRequest(final ProductRecommendationRequest request) {
        if (request.memberId() == null || request.memberId() <= 0) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }

        if (request.userDiagnosis() == null || request.userDiagnosis().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.AI_INVALID_REQUEST_FORMAT);
        }

        if (request.topK() == null || request.topK() <= 0 || request.topK() > maxRecommendations) {
            throw new BusinessException(ErrorCode.AI_INVALID_REQUEST_FORMAT);
        }
    }

    private boolean isConnectionRefused(final RestClientException exception) {
        return exception.getMessage() != null && exception.getMessage().contains("Connection refused");
    }

    private boolean isTimeoutException(final RestClientException exception) {
        return exception.getCause() instanceof java.net.SocketTimeoutException ||
               (exception.getMessage() != null && exception.getMessage().contains("timeout"));
    }

    /**
     * AI 상품 추천 요청 정보.
     */
    public record ProductRecommendationRequest(
            Long memberId,
            String userDiagnosis,
            Integer topK,
            Map<String, Object> memberProfile
    ) {
        public static ProductRecommendationRequest of(final Long memberId, final String userDiagnosis,
                                                      final Map<String, Object> memberProfile) {
            return new ProductRecommendationRequest(memberId, userDiagnosis, 10, memberProfile);
        }
    }

    /**
     * AI 상품 추천 응답 정보.
     */
    public record ProductRecommendationResponse(
            List<Map<String, Object>> recommendations,
            Integer totalCount,
            Double processingTimeMs,
            Map<String, Object> requestInfo
    ) {}
}