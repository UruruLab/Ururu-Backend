package com.ururulab.ururu.payment.domain.dto.request;

public record TossWebhookDto(
        String eventType,
        TossWebhookDataDto data
) {
}