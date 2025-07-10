package com.ururulab.ururu.ai.controller;

import com.ururulab.ururu.ai.dto.GroupBuyRecommendationRequest;
import com.ururulab.ururu.ai.dto.GroupBuyRecommendationResponse;
import com.ururulab.ururu.ai.service.GroupBuyRecommendationService;
import com.ururulab.ururu.global.domain.dto.ApiResponseFormat;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai/groupbuy-recommendations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI 공동구매 추천", description = "AI 기반 개인 맞춤형 공동구매 상품 추천 API")
public class GroupBuyRecommendationController {

    private final GroupBuyRecommendationService recommendationService;

    @Operation(
            summary = "개인 맞춤형 공동구매 추천",
            description = "회원의 뷰티 프로필을 기반으로 AI가 개인 맞춤형 공동구매 상품을 추천합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "추천 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "추천 가능한 상품 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "423", description = "중복 요청 처리 중"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "AI 서비스 이용 불가")
    })
    @PostMapping
    public ResponseEntity<ApiResponseFormat<GroupBuyRecommendationResponse>> getGroupBuyRecommendations(
            @Parameter(description = "공동구매 추천 요청 정보", required = true)
            @Valid @RequestBody final GroupBuyRecommendationRequest request
    ) {
        log.info("공동구매 추천 API 호출 - 회원ID: {}, 피부타입: {}",
                request.memberId(), request.skinType());

        final GroupBuyRecommendationResponse response = recommendationService.getRecommendations(request);

        log.info("공동구매 추천 API 응답 완료 - 회원ID: {}, 추천 수: {}",
                request.memberId(), response.recommendedGroupBuys().size());

        return ResponseEntity.ok(ApiResponseFormat.success("공동구매 추천이 완료되었습니다.", response));
    }

    @Operation(
            summary = "추천 캐시 갱신",
            description = "특정 회원의 추천 캐시를 백그라운드에서 갱신합니다."
    )
    @PostMapping("/{memberId}/refresh")
    public ResponseEntity<ApiResponseFormat<Void>> refreshRecommendationCache(
            @Parameter(description = "회원 ID", required = true, example = "1")
            @PathVariable final Long memberId,

            @Parameter(description = "공동구매 추천 요청 정보", required = true)
            @Valid @RequestBody final GroupBuyRecommendationRequest request
    ) {
        log.info("추천 캐시 갱신 API 호출 - 회원ID: {}", memberId);

        recommendationService.refreshRecommendationCache(request);

        return ResponseEntity.ok(ApiResponseFormat.success("추천 캐시 갱신이 시작되었습니다."));
    }

    @Operation(
            summary = "개별 회원 추천 캐시 삭제",
            description = "특정 회원의 추천 캐시를 삭제합니다."
    )
    @DeleteMapping("/{memberId}/cache")
    public ResponseEntity<ApiResponseFormat<Void>> evictMemberRecommendationCache(
            @Parameter(description = "회원 ID", required = true, example = "1")
            @PathVariable final Long memberId
    ) {
        log.info("개별 추천 캐시 삭제 API 호출 - 회원ID: {}", memberId);

        recommendationService.evictRecommendationCache(memberId);

        return ResponseEntity.ok(ApiResponseFormat.success("추천 캐시가 삭제되었습니다."));
    }

    @Operation(
            summary = "전체 추천 캐시 삭제",
            description = "모든 회원의 추천 캐시를 삭제합니다. (관리자 전용)"
    )
    @DeleteMapping("/cache/all")
    public ResponseEntity<ApiResponseFormat<Void>> evictAllRecommendationCaches() {
        log.info("전체 추천 캐시 삭제 API 호출");

        recommendationService.evictAllRecommendationCaches();

        return ResponseEntity.ok(ApiResponseFormat.success("전체 추천 캐시가 삭제되었습니다."));
    }
}
