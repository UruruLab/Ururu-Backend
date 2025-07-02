package com.ururulab.ururu.payment.domain.dto.response;

public record PaymentFailResponseDto(
        String errorCode,
        String errorMessage,
        String orderId
) {
}