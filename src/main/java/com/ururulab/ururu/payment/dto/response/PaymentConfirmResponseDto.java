package com.ururulab.ururu.payment.dto.response;

import com.ururulab.ururu.payment.domain.entity.enumerated.PaymentStatus;

import java.time.Instant;

public record PaymentConfirmResponseDto(
        Long paymentId,
        PaymentStatus status,
        Instant paidAt
) {
}
