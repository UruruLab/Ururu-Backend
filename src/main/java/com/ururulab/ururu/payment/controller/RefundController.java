package com.ururulab.ururu.payment.controller;

import com.ururulab.ururu.global.domain.dto.ApiResponseFormat;
import com.ururulab.ururu.payment.dto.request.RefundProcessRequestDto;
import com.ururulab.ururu.payment.dto.request.RefundRequestDto;
import com.ururulab.ururu.payment.dto.response.RefundCreateResponseDto;
import com.ururulab.ururu.payment.dto.response.RefundProcessResponseDto;
import com.ururulab.ururu.payment.service.RefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "환불", description = "환불 요청 및 처리 API")
public class RefundController {

    private final RefundService refundService;

    @Operation(summary = "환불 요청 생성", description = "주문에 대한 수동 환불을 요청합니다. Order 단위 전체 환불만 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "환불 요청 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터 또는 환불 불가 상태"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음 (다른 회원의 주문)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 주문 또는 결제"),
            @ApiResponse(responseCode = "409", description = "이미 진행 중인 환불 요청 또는 중복 환불")
    })
    @PostMapping("/orders/{orderId}/refund")
    public ResponseEntity<ApiResponseFormat<RefundCreateResponseDto>> createRefundRequest(
            @AuthenticationPrincipal Long memberId,
            @PathVariable String orderId,
            @Valid @RequestBody RefundRequestDto request
    ) {
        log.debug("환불 요청 생성 - 회원ID: {}, 주문ID: {}, 요청: {}", memberId, orderId, request);

        RefundCreateResponseDto response = refundService.createRefundRequest(memberId, orderId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponseFormat.success("환불 요청이 생성되었습니다", response));
    }

    @Operation(summary = "환불 요청 처리", description = "판매자가 환불 요청을 승인 또는 거절합니다. 승인 시 실제 PG 환불이 진행됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "환불 처리 완료"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터 또는 처리 불가 상태"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음 (다른 판매자의 환불)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 환불"),
            @ApiResponse(responseCode = "409", description = "이미 처리된 환불"),
            @ApiResponse(responseCode = "500", description = "PG 환불 처리 실패")
    })
    @PatchMapping("/refunds/{refundId}")
    public ResponseEntity<ApiResponseFormat<RefundProcessResponseDto>> processRefundRequest(
            @AuthenticationPrincipal Long sellerId,
            @PathVariable String refundId,
            @Valid @RequestBody RefundProcessRequestDto request
    ) {
        log.debug("환불 요청 처리 - 판매자ID: {}, 환불ID: {}, 액션: {}", sellerId, refundId, request.action());

        RefundProcessResponseDto response = refundService.processRefundRequest(sellerId, refundId, request);

        String message = "APPROVE".equals(request.action()) ? "환불이 승인되었습니다" : "환불이 거절되었습니다";

        return ResponseEntity.ok(
                ApiResponseFormat.success(message, response)
        );
    }
}