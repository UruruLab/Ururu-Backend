package com.ururulab.ururu.order.controller;

import com.ururulab.ururu.global.domain.dto.ApiResponseFormat;
import com.ururulab.ururu.order.dto.response.MyOrderListResponseDto;
import com.ururulab.ururu.order.service.MyOrderService;
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
@Tag(name = "주문/배송 내역", description = "주문/배송 내역 조회 API")
public class MyOrderController {

    private final MyOrderService myOrderService;

    @Operation(summary = "나의 주문 목록 조회", description = "회원의 주문 목록을 조회합니다. 환불 신청(INITIATED) 단계까지만 포함하며, 상태별 필터링과 페이징을 지원합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "주문 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 회원")
    })
    @GetMapping("/orders")
    public ResponseEntity<ApiResponseFormat<MyOrderListResponseDto>> getMyOrders(
            @AuthenticationPrincipal Long memberId,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        log.debug("나의 주문 목록 조회 요청 - 회원ID: {}, 상태: {}, 페이지: {}, 크기: {}",
                memberId, status, page, size);

        MyOrderListResponseDto response = myOrderService.getMyOrders(memberId, status, page, size);

        return ResponseEntity.ok(
                ApiResponseFormat.success("주문/배송 내역 조회 성공", response)
        );
    }
}