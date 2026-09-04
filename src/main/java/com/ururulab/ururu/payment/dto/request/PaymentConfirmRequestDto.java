package com.ururulab.ururu.payment.dto.request;

import com.ururulab.ururu.payment.dto.validation.PaymentValidationMessages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record PaymentConfirmRequestDto(
        @NotBlank(message = PaymentValidationMessages.PAYMENT_KEY_REQUIRED)
        @Size(max = 200)
        String paymentKey,

        @NotNull(message = PaymentValidationMessages.AMOUNT_REQUIRED)
        @PositiveOrZero
        Integer amount
) {
}
