package com.ururulab.ururu.payment.dto.request;

public record TossWebhookDataDto(
        String paymentKey,
        String orderId,
        String status
) {
}
