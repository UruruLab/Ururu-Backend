package com.ururulab.ururu.order.dto.response;

import java.time.Instant;
import java.util.List;

public record MyOrderResponseDto(
        String orderId,
        Instant createdAt,
        String trackingNumber,
        Integer totalAmount,
        List<OrderItemResponseDto> orderItems
) {
}
