package com.ururulab.ururu.order.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ururulab.ururu.payment.domain.entity.enumerated.RefundType;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MyOrderResponseDto(
        String orderId,
        Instant createdAt,
        String trackingNumber,
        Integer totalAmount,
        Boolean canRefundChangeOfMind,
        Boolean canRefundOthers,
        RefundType refundType,     // 환불 INIT 경우
        String refundReason,       // 환불 INIT 경우
        List<OrderItemResponseDto> orderItems
) {
}
