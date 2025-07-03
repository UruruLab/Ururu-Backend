package com.ururulab.ururu.payment.dto.request;

import com.ururulab.ururu.payment.dto.validation.PaymentValidationMessages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentConfirmRequestDto(
        @NotBlank(message = PaymentValidationMessages.PAYMENT_KEY_REQUIRED)
        String paymentKey,

        @NotNull(message = PaymentValidationMessages.AMOUNT_REQUIRED)
        Integer amount
) {
}
