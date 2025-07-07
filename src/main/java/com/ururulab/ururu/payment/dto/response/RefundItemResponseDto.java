package com.ururulab.ururu.payment.dto.response;

public record RefundItemResponseDto(
        Long groupbuyOptionId,
        Long productOptionId,
        String optionImage,
        String productName,
        String optionName,
        Integer quantity,
        Integer price
) {
}