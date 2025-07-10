package com.ururulab.ururu.ai.service;

import com.ururulab.ururu.ai.dto.GroupBuyRecommendationRequest;
import com.ururulab.ururu.ai.dto.GroupBuyRecommendationResponse;
import com.ururulab.ururu.ai.dto.GroupBuyRecommendationResponse.RecommendedGroupBuy;
import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.global.exception.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("공동구매 추천 서비스 Mock 테스트")
class GroupBuyRecommendationServiceMockTest {

    @Mock
    private GroupBuyRecommendationCacheService cacheService;

    @Mock
    private UruruAiService aiService;

    @InjectMocks
    private GroupBuyRecommendationService recommendationService;

    @Test
    @DisplayName("캐시에서 추천 결과를 찾으면 AI 서비스를 호출하지 않는다")
    void shouldReturnCachedResultWhenCacheHit() {
        // Given
        final Long memberId = 1L;
        final GroupBuyRecommendationRequest request = createMockRequest(memberId);
        final GroupBuyRecommendationResponse cachedResponse = createMockResponse();

        given(cacheService.getCachedRecommendation(memberId)).willReturn(cachedResponse);

        // When
        final GroupBuyRecommendationResponse result = recommendationService.getRecommendations(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.cacheSource()).isEqualTo("CACHE");
        then(aiService).should(never()).getRecommendations(any());
        then(cacheService).should(never()).tryAcquireProcessingLock(any());
    }

    @Test
    @DisplayName("캐시 미스 시 AI 서비스를 호출하고 결과를 캐시에 저장한다")
    void shouldCallAiServiceAndCacheResultWhenCacheMiss() {
        // Given
        final Long memberId = 1L;
        final GroupBuyRecommendationRequest request = createMockRequest(memberId);
        final List<RecommendedGroupBuy> mockRecommendations = createMockRecommendations();

        given(cacheService.getCachedRecommendation(memberId)).willReturn(null);
        given(cacheService.tryAcquireProcessingLock(memberId)).willReturn(true);
        given(aiService.getRecommendations(request)).willReturn(mockRecommendations);

        // When
        final GroupBuyRecommendationResponse result = recommendationService.getRecommendations(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.recommendedGroupBuys()).hasSize(2);
        assertThat(result.cacheSource()).isEqualTo("AI_SERVICE");
        then(cacheService).should().cacheRecommendation(any(), any());
        then(cacheService).should().releaseProcessingLock(memberId);
    }

    @Test
    @DisplayName("처리 락 획득 실패 시 중복 요청 예외를 던진다")
    void shouldThrowExceptionWhenProcessingLockFailed() {
        // Given
        final Long memberId = 1L;
        final GroupBuyRecommendationRequest request = createMockRequest(memberId);

        given(cacheService.getCachedRecommendation(memberId)).willReturn(null);
        given(cacheService.tryAcquireProcessingLock(memberId)).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> recommendationService.getRecommendations(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_PROCESSING_IN_PROGRESS);

        then(aiService).should(never()).getRecommendations(any());
    }

    @Test
    @DisplayName("AI 서비스에서 빈 결과를 반환하면 예외를 던진다")
    void shouldThrowExceptionWhenAiServiceReturnsEmptyResult() {
        // Given
        final Long memberId = 1L;
        final GroupBuyRecommendationRequest request = createMockRequest(memberId);

        given(cacheService.getCachedRecommendation(memberId)).willReturn(null);
        given(cacheService.tryAcquireProcessingLock(memberId)).willReturn(true);
        given(aiService.getRecommendations(request)).willReturn(List.of());

        // When & Then
        assertThatThrownBy(() -> recommendationService.getRecommendations(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AI_NO_RECOMMENDATIONS_FOUND);

        then(cacheService).should().releaseProcessingLock(memberId);
    }

    private GroupBuyRecommendationRequest createMockRequest(final Long memberId) {
        return GroupBuyRecommendationRequest.ofBeautyProfile(
                memberId,
                "건성",
                List.of("수분부족", "각질"),
                List.of("스킨케어"),
                "20대",
                "10000-30000원",
                10
        );
    }

    private GroupBuyRecommendationResponse createMockResponse() {
        return GroupBuyRecommendationResponse.of(
                createMockRecommendations(),
                "테스트 추천 이유",
                LocalDateTime.now(),
                "AI_SERVICE"
        );
    }

    private List<RecommendedGroupBuy> createMockRecommendations() {
        return List.of(
                RecommendedGroupBuy.of(
                        1L, "테스트 공동구매 1", 1L, "테스트 상품 1",
                        new BigDecimal("30000"), new BigDecimal("25000"),
                        "test-image-1.jpg", 0.95, "스킨케어",
                        List.of("히알루론산", "세라마이드"), "수분 공급에 탁월",
                        15, 10, LocalDateTime.now().plusDays(7)
                ),
                RecommendedGroupBuy.of(
                        2L, "테스트 공동구매 2", 2L, "테스트 상품 2",
                        new BigDecimal("25000"), new BigDecimal("20000"),
                        "test-image-2.jpg", 0.89, "마스크팩",
                        List.of("나이아신아마이드", "비타민C"), "각질 제거 효과",
                        8, 5, LocalDateTime.now().plusDays(5)
                )
        );
    }
}
