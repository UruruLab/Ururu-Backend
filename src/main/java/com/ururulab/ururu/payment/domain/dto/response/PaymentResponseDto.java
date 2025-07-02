package com.ururulab.ururu.payment.domain.dto.response;

public record PaymentResponseDto(
        Long paymentId,
        String orderId,
        Integer amount,
        String orderName,
        String customerName
) {
}