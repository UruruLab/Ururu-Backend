package com.ururulab.ururu.payment.controller.dto.response;

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