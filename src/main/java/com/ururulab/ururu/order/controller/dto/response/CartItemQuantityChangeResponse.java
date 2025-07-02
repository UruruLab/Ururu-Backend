package com.ururulab.ururu.order.controller.dto.response;

/**
 * 장바구니 아이템 수량 변경 응답 DTO
 * PUT /cart/items/{cartItemId} 응답
 */
public record CartItemQuantityChangeResponse(
        Long cartItemId,
        Integer quantity
) {
}
