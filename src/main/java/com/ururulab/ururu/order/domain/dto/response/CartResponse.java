package com.ururulab.ururu.order.domain.dto.response;

import java.util.List;

/**
 * 장바구니 조회 응답 DTO
 * GET /cart 응답
 */
public record CartResponse(
        List<CartItemResponse> cartItems
) {
}