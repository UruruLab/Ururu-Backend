package com.ururulab.ururu.order.controller;

import com.ururulab.ururu.global.domain.dto.ApiResponseFormat;
import com.ururulab.ururu.order.dto.request.ShippingInfoUpdateRequest;
import com.ururulab.ururu.order.dto.response.ShippingInfoUpdateResponse;
import com.ururulab.ururu.order.service.OrderManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "주문 관리", description = "판매자용 주문 관리 API")
public class OrderManagementController {

    private final OrderManagementService orderManagementService;

    @Operation(summary = "배송 정보 등록",
            description = "판매자가 주문에 대한 배송 정보(운송장 번호)를 등록합니다. " +
                    "ORDERED 상태의 주문에만 등록 가능하며, 이미 등록된 경우 수정할 수 없습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "배송 정보 등록 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터 또는 이미 등록된 운송장"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음 (다른 판매자의 주문)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 주문")
    })
    @PatchMapping("/orders/{orderId}/shipping")
    public ResponseEntity<ApiResponseFormat<ShippingInfoUpdateResponse>> updateShippingInfo(
            @AuthenticationPrincipal Long sellerId,
            @PathVariable String orderId,
            @Valid @RequestBody ShippingInfoUpdateRequest request
    ) {
        log.debug("배송 정보 등록 요청 - 판매자ID: {}, 주문ID: {}", sellerId, orderId);

        ShippingInfoUpdateResponse response = orderManagementService.updateShippingInfo(sellerId, orderId, request);

        return ResponseEntity.ok(
                ApiResponseFormat.success("배송 정보가 등록되었습니다", response)
        );
    }
}
