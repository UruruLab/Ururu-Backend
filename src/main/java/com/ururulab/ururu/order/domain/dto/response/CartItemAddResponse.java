package com.ururulab.ururu.order.domain.dto.response;

/**
 * 장바구니 아이템 추가 응답 DTO
 * POST /cart/items 응답
 */
public record CartItemAddResponse(
        Long cartItemId,
        Integer quantity
) {
}
