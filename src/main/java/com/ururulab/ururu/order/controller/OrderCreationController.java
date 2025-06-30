package com.ururulab.ururu.order.controller;

import com.ururulab.ururu.global.domain.dto.ApiResponseFormat;
import com.ururulab.ururu.order.domain.dto.request.CartOrderCreateRequest;
import com.ururulab.ururu.order.domain.dto.request.GroupBuyOrderCreateRequest;
import com.ururulab.ururu.order.domain.dto.response.OrderCreateResponse;
import com.ururulab.ururu.order.service.OrderCreationService;
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
@Tag(name = "주문 생성", description = "주문 생성 관련 API")
public class OrderCreationController {

    private final OrderCreationService orderCreationService;

    @Operation(summary = "공구 주문서 생성", description = "특정 공구의 옵션들로 주문서를 생성합니다. 주문서 생성 후 30분 내 결제가 필요합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "주문서 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 공구 또는 옵션"),
            @ApiResponse(responseCode = "409", description = "재고 부족 또는 개인 구매 제한 초과"),
            @ApiResponse(responseCode = "423", description = "종료된 공구 또는 이미 진행 중인 주문")
    })
    @PostMapping("/api/groupbuys/{groupbuyId}/orders")
    public ResponseEntity<ApiResponseFormat<OrderCreateResponse>> createGroupBuyOrder(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long groupbuyId,
            @Valid @RequestBody GroupBuyOrderCreateRequest request
    ) {
        log.debug("공구 주문서 생성 요청 - 회원ID: {}, 공구ID: {}, 요청: {}", memberId, groupbuyId, request);

        OrderCreateResponse response = orderCreationService.createGroupBuyOrder(memberId, groupbuyId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponseFormat.success("주문서가 생성되었습니다", response));
    }

    @Operation(summary = "장바구니 주문서 생성", description = "장바구니의 선택된 아이템들로 주문서를 생성합니다. 주문서 생성 후 30분 내 결제가 필요합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "주문서 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 장바구니 아이템"),
            @ApiResponse(responseCode = "409", description = "재고 부족 또는 개인 구매 제한 초과"),
            @ApiResponse(responseCode = "423", description = "종료된 공구 또는 이미 진행 중인 주문")
    })
    @PostMapping("/api/cart/orders")
    public ResponseEntity<ApiResponseFormat<OrderCreateResponse>> createCartOrder(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody CartOrderCreateRequest request
    ) {
        log.debug("장바구니 주문서 생성 요청 - 회원ID: {}, 요청: {}", memberId, request);

        OrderCreateResponse response = orderCreationService.createCartOrder(memberId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponseFormat.success("주문서가 생성되었습니다", response));
    }
}