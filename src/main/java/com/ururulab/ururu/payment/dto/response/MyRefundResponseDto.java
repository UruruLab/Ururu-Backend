package com.ururulab.ururu.payment.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ururulab.ururu.payment.domain.entity.enumerated.RefundStatus;
import com.ururulab.ururu.payment.domain.entity.enumerated.RefundType;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MyRefundResponseDto(
        String refundId,
        Instant createdAt,
        RefundType type,            // 환불 타입: "GROUPBUY_FAILED", CHANGE_OF_MIND, DEFECTIVE_PRODUCT, DELIVERY_ISSUE, OTHER
        String reason,              // 환불 사유
        RefundStatus status,  // 환불 상태: INITIATED, APPROVED, REJECTED, COMPLETED, FAILED
        String rejectionReason,     // 환불 거절 사유
        Instant refundAt,           // 환불 확정 일시
        Integer totalAmount,
        List<RefundItemResponseDto> refundItems
) {
}