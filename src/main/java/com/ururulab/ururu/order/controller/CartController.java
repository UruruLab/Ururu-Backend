package com.ururulab.ururu.order.controller;

import com.ururulab.ururu.global.domain.dto.ApiResponseFormat;
import com.ururulab.ururu.order.domain.dto.request.CartItemAddRequest;
import com.ururulab.ururu.order.domain.dto.request.CartItemQuantityChangeRequest;
import com.ururulab.ururu.order.domain.dto.response.CartItemAddResponse;
import com.ururulab.ururu.order.domain.dto.response.CartResponse;
import com.ururulab.ururu.order.domain.dto.response.CartItemQuantityChangeResponse;
import com.ururulab.ururu.order.service.CartService;
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
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "장바구니", description = "장바구니 관리 API")
public class CartController {

    private final CartService cartService;

    @Operation(summary = "장바구니 아이템 추가", description = "공구 옵션을 장바구니에 추가합니다. 이미 존재하는 옵션인 경우 수량이 증가됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "장바구니 추가 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 공구 옵션"),
            @ApiResponse(responseCode = "409", description = "종료된 공구")
    })
    @PostMapping("/items")
    public ResponseEntity<ApiResponseFormat<CartItemAddResponse>> addCartItem(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody CartItemAddRequest request
    ) {
        log.debug("장바구니 아이템 추가 요청 - 회원ID: {}, 요청: {}", memberId, request);

        CartItemAddResponse response = cartService.addCartItem(memberId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponseFormat.success("장바구니에 추가되었습니다", response));
    }

    @Operation(summary = "장바구니 조회", description = "회원의 장바구니를 조회합니다. 만료된 공구는 자동으로 제외됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "장바구니 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping
    public ResponseEntity<ApiResponseFormat<CartResponse>> getCart(
            @AuthenticationPrincipal Long memberId
    ) {
        log.debug("장바구니 조회 요청 - 회원ID: {}", memberId);

        CartResponse response = cartService.getCart(memberId);

        return ResponseEntity.ok(
                ApiResponseFormat.success("장바구니 조회 성공", response)
        );
    }

    @Operation(summary = "장바구니 아이템 수량 변경", description = "장바구니 아이템의 수량을 변경합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수량 변경 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 장바구니 아이템"),
            @ApiResponse(responseCode = "403", description = "다른 사용자의 장바구니 아이템")
    })
    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponseFormat<CartItemQuantityChangeResponse>> updateCartItemQuantity(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long cartItemId,
            @Valid @RequestBody CartItemQuantityChangeRequest request
    ) {
        log.debug("장바구니 아이템 수량 변경 요청 - 회원ID: {}, 아이템ID: {}, 요청: {}",
                memberId, cartItemId, request);

        CartItemQuantityChangeResponse response = cartService.updateCartItemQuantity(memberId, cartItemId, request);

        return ResponseEntity.ok(
                ApiResponseFormat.success("수량이 변경되었습니다", response)
        );
    }

    @Operation(summary = "장바구니 아이템 삭제", description = "장바구니에서 특정 아이템을 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 장바구니 아이템"),
            @ApiResponse(responseCode = "403", description = "다른 사용자의 장바구니 아이템")
    })
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponseFormat<Void>> removeCartItem(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long cartItemId
    ) {
        log.debug("장바구니 아이템 삭제 요청 - 회원ID: {}, 아이템ID: {}", memberId, cartItemId);

        cartService.removeCartItem(memberId, cartItemId);

        return ResponseEntity.ok(
                ApiResponseFormat.success("장바구니에서 삭제되었습니다")
        );
    }
}