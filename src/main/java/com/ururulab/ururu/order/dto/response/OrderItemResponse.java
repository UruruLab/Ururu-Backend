package com.ururulab.ururu.order.dto.response;

/**
 * 주문 아이템 정보 응답 DTO
 */
public record OrderItemResponse(
        Long groupbuyOptionId,
        Integer quantity,
        String productName,
        String optionName,
        Integer price
) {
}
