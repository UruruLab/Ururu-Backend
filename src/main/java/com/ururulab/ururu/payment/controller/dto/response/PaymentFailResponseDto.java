package com.ururulab.ururu.payment.controller.dto.response;

public record PaymentFailResponseDto(
        String errorCode,
        String errorMessage,
        String orderId
) {
}