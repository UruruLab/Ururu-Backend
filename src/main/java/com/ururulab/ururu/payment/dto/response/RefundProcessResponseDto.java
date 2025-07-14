package com.ururulab.ururu.payment.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ururulab.ururu.payment.domain.entity.enumerated.RefundStatus;

/**
 * 환불 처리 응답 DTO
 * PATCH /api/refunds/{refundId} 응답
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RefundProcessResponseDto(
        String refundId,
        String orderId,
        RefundStatus status,
        String rejectReason  // REJECT 일때
) {
}