package com.ururulab.ururu.payment.dto.request;

public record TossWebhookDto(
        String eventType,
        TossWebhookDataDto data
) {
}