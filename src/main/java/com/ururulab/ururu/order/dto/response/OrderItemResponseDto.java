package com.ururulab.ururu.order.dto.response;

public record OrderItemResponseDto(
        Long groupbuyOptionId,
        Long productOptionId,
        String status,        // 공구 상태: "OPEN", "SUCCESS", "FAIL"
        Integer rate,         // 할인율
        String optionImage,
        String productName,
        String optionName,
        Integer quantity,
        Integer price
) {
}