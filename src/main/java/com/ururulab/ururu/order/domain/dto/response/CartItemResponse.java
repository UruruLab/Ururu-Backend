package com.ururulab.ururu.order.domain.dto.response;

import com.ururulab.ururu.order.domain.entity.CartItem;

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
    public static CartItemResponse from(final CartItem cartItem) {
        return new CartItemResponse(
                cartItem.getId(),
                cartItem.getGroupBuyOption().getId(),
                cartItem.getQuantity(),
                cartItem.getGroupBuyOption().getProductOption().getProduct().getName(),
                cartItem.getGroupBuyOption().getProductOption().getName(),
                cartItem.getGroupBuyOption().getProductOption().getImageUrl(),
                cartItem.getGroupBuyOption().getProductOption().getPrice(),
                cartItem.getGroupBuyOption().getGroupBuy().getEndsAt().toInstant()
        );
    }
}