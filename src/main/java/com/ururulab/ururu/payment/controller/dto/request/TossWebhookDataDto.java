package com.ururulab.ururu.payment.controller.dto.request;

public record TossWebhookDataDto(
        String paymentKey,
        String orderId,
        String status
) {
}
