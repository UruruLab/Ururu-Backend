package com.ururulab.ururu.payment.dto.response;

import com.ururulab.ururu.payment.domain.entity.enumerated.RefundStatus;
import com.ururulab.ururu.payment.domain.entity.enumerated.RefundType;

/**
 * 환불 생성 응답 DTO
 * POST /api/orders/{orderId}/refund 응답
 */
public record RefundCreateResponseDto(
        String refundId,
        RefundStatus status,
        RefundType type,
        Integer amount
) {
}
