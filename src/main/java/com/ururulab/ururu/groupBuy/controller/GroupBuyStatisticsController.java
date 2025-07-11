package com.ururulab.ururu.groupBuy.controller;

import com.ururulab.ururu.global.domain.dto.ApiResponseFormat;
import com.ururulab.ururu.groupBuy.dto.response.GroupBuyStatisticsDetailResponse;
import com.ururulab.ururu.groupBuy.dto.response.GroupBuyStatisticsResponse;
import com.ururulab.ururu.groupBuy.service.GroupBuyStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/groupbuy/{sellerId}/statistics")
@RequiredArgsConstructor
public class GroupBuyStatisticsController {

    private final GroupBuyStatisticsService groupBuyStatisticsService;

    @Operation(
            summary = "특정 공동구매 상세 통계 조회",
            description = "판매자가 자신의 특정 공동구매에 대한 상세 통계 정보를 조회합니다. 옵션별 주문 정보를 포함합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "공동구매 상세 통계 조회 성공"),
            @ApiResponse(responseCode = "403", description = "접근 권한이 없습니다."),
            @ApiResponse(responseCode = "404", description = """
                    - 공동구매 통계를 찾을 수 없습니다.
                    - 해당 공동구매를 찾을 수 없습니다.
                    """),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/{groupBuyId}")
    public ResponseEntity<ApiResponseFormat<GroupBuyStatisticsDetailResponse>> getGroupBuyStatisticsDetail(
            @PathVariable Long groupBuyId,
            @PathVariable Long sellerId
    ) {

        //TODO @PathVariable Long sellerId 삭제 후 주석 해제
        //Long sellerId = AuthUtils.getSellerIdFromAuthentication();

        GroupBuyStatisticsDetailResponse response = groupBuyStatisticsService.getGroupBuyStatisticsDetail(groupBuyId, sellerId);
        return ResponseEntity.ok(ApiResponseFormat.success("공동구매 상세 통계 조회에 성공하였습니다.", response));
    }

    @Operation(
            summary = "판매자의 모든 공동구매 통계 조회",
            description = "판매자가 자신의 모든 공동구매에 대한 기본 통계 목록을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "공동구매 통계 목록 조회 성공"),
            @ApiResponse(responseCode = "404", description = "공동구매 통계를 찾을 수 없습니다."),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping
    public ResponseEntity<ApiResponseFormat<List<GroupBuyStatisticsResponse>>> getMyGroupBuyStatistics(
            @PathVariable Long sellerId
    ) {
        //TODO @PathVariable Long sellerId 삭제 후 주석 해제
        //Long sellerId = AuthUtils.getSellerIdFromAuthentication();

        List<GroupBuyStatisticsResponse> response = groupBuyStatisticsService.getGroupBuyStatisticsBySeller(sellerId);
        return ResponseEntity.ok(ApiResponseFormat.success("공동구매 통계 목록 조회에 성공하였습니다.", response));
    }
}
