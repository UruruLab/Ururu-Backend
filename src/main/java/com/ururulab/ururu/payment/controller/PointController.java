package com.ururulab.ururu.payment.controller;

import com.ururulab.ururu.global.domain.dto.ApiResponseFormat;
import com.ururulab.ururu.payment.dto.response.MemberPointResponse;
import com.ururulab.ururu.payment.dto.response.PointTransactionListResponse;
import com.ururulab.ururu.payment.service.PointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member/me")
@Tag(name = "포인트", description = "포인트 관련 API")
public class PointController {

    private final PointService pointService;

    @Operation(summary = "내 포인트 조회", description = "현재 보유한 포인트를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "포인트 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 회원")
    })
    @GetMapping("/points")
    public ResponseEntity<ApiResponseFormat<MemberPointResponse>> getCurrentPoints(
            @AuthenticationPrincipal Long memberId
    ) {
        log.debug("포인트 조회 요청 - 회원ID: {}", memberId);

        MemberPointResponse response = pointService.getCurrentPoints(memberId);

        return ResponseEntity.ok(
                ApiResponseFormat.success("포인트 조회 성공", response)
        );
    }

    @Operation(summary = "포인트 사용 내역 조회", description = "포인트 적립/사용 내역을 조회합니다. 타입과 소스로 필터링 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "포인트 내역 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 회원")
    })
    @GetMapping("/point-transactions")
    public ResponseEntity<ApiResponseFormat<PointTransactionListResponse>> getPointTransactions(
            @AuthenticationPrincipal Long memberId,
            @RequestParam(defaultValue = "all") String type,
            @RequestParam(defaultValue = "all") String source,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.debug("포인트 내역 조회 요청 - 회원ID: {}, 타입: {}, 소스: {}, 페이지: {}, 크기: {}",
                memberId, type, source, page, size);

        PointTransactionListResponse response = pointService.getPointTransactions(
                memberId, type, source, page, size);

        return ResponseEntity.ok(
                ApiResponseFormat.success("포인트 내역 조회 성공", response)
        );
    }
}