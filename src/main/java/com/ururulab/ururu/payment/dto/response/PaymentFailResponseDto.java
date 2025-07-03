package com.ururulab.ururu.payment.dto.response;

public record PaymentFailResponseDto(
        String errorCode,
        String errorMessage,
        String orderId
) {
}