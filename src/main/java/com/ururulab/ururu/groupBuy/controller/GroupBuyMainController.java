package com.ururulab.ururu.groupBuy.controller;

import com.ururulab.ururu.global.domain.dto.ApiResponseFormat;
import com.ururulab.ururu.groupBuy.dto.response.GroupBuyListResponse;
import com.ururulab.ururu.groupBuy.service.GroupBuyMainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/groupbuys")
@RequiredArgsConstructor
@Slf4j
public class GroupBuyMainController {

    private final GroupBuyMainService groupBuyMainService;

    @Operation(
            summary = "실시간 베스트 공동구매 조회",
            description = "메인 화면에 표시될 판매량 기준 상위 3개 공동구매를 조회합니다. 캐시를 통해 빠른 응답을 제공합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "실시간 베스트 공동구매 조회 성공"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/top3")
    public ResponseEntity<ApiResponseFormat<List<GroupBuyListResponse>>> getRealtimeBest() {
        log.debug("GET /groupbuy/top3 - 실시간 베스트 공동구매 조회");

        List<GroupBuyListResponse> bestList = groupBuyMainService.getRealtimeBestGroupBuys();

        return ResponseEntity.ok(ApiResponseFormat.success("실시간 베스트 공동구매 조회에 성공하였습니다.", bestList));
    }

    @Operation(
            summary = "카테고리별 인기 공동구매 조회",
            description = "메인 화면에 표시될 특정 카테고리의 판매량 기준 상위 6개 공동구매를 조회합니다. 캐시를 통해 빠른 응답을 제공합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "카테고리별 인기 공동구매 조회 성공"),
            @ApiResponse(responseCode = "400", description = "해당 카테고리를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/{categoryId}/top6")
    public ResponseEntity<ApiResponseFormat<List<GroupBuyListResponse>>> getCategoryPopular(
            @PathVariable Long categoryId) {
        log.debug("GET /groupbuy/{}/top6 - 카테고리별 인기 공동구매 조회", categoryId);

        List<GroupBuyListResponse> popularList = groupBuyMainService.getCategoryPopularGroupBuys(categoryId);

        return ResponseEntity.ok(ApiResponseFormat.success("카테고리별 인기 공동구매 조회에 성공하였습니다.", popularList));
    }
}
