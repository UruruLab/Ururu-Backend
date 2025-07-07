package com.ururulab.ururu.payment.controller;

import com.ururulab.ururu.global.domain.dto.ApiResponseFormat;
import com.ururulab.ururu.payment.dto.response.MyRefundListResponseDto;
import com.ururulab.ururu.payment.service.MyRefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member/me")
@Tag(name = "취소/환불 내역", description = "취소/환불 내역 조회 API")
public class MyRefundController {

    private final MyRefundService myRefundService;

    @Operation(summary = "나의 환불 내역 조회", description = "회원의 환불 내역을 조회합니다. 환불 처리 과정(INITIATED 이후)만 포함하며, 상태별 필터링과 페이징을 지원합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "환불 내역 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 회원")
    })
    @GetMapping("/refunds")
    public ResponseEntity<ApiResponseFormat<MyRefundListResponseDto>> getMyRefunds(
            @AuthenticationPrincipal Long memberId,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        log.debug("나의 환불 내역 조회 요청 - 회원ID: {}, 상태: {}, 페이지: {}, 크기: {}",
                memberId, status, page, size);

        MyRefundListResponseDto response = myRefundService.getMyRefunds(memberId, status, page, size);

        return ResponseEntity.ok(
                ApiResponseFormat.success("취소/반품 내역 조회 성공", response)
        );
    }
}