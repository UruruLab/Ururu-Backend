package com.ururulab.ururu.payment.controller.dto.request;

public record TossWebhookDto(
        String eventType,
        TossWebhookDataDto data
) {
}