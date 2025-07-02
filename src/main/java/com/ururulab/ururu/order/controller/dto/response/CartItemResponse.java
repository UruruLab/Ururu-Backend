package com.ururulab.ururu.order.controller.dto.response;

import java.time.Instant;

/**
 * 장바구니 아이템 정보 DTO
 * GET /cart 응답의 cartItems 배열 요소
 */
public record CartItemResponse(
        Long cartItemId,
        Long groupbuyOptionId,
        Integer quantity,
        String productName,
        String optionName,
        String optionImage,
        Integer price,
        Instant endsAt
) {
}