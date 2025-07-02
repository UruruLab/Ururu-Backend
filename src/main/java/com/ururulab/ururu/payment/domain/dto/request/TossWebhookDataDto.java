package com.ururulab.ururu.payment.domain.dto.request;

public record TossWebhookDataDto(
        String paymentKey,
        String orderId,
        String status
) {
}
