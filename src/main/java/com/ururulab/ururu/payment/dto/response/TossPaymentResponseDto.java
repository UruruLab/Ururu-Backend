package com.ururulab.ururu.payment.dto.response;

public record TossPaymentResponseDto(
        String paymentKey,
        String orderId,
        String method,
        String easyPayProvider,
        String status,
        String approvedAt,
        Integer totalAmount
) {
}