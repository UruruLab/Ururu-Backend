package com.ururulab.ururu.payment.controller.dto.request;

import com.ururulab.ururu.payment.controller.dto.validation.PaymentValidationConstants;
import com.ururulab.ururu.payment.controller.dto.validation.PaymentValidationMessages;
import jakarta.validation.constraints.*;

public record PaymentRequestDto(
        @NotBlank(message = PaymentValidationMessages.ORDER_ID_REQUIRED)
        String orderId,

        @NotNull(message = PaymentValidationMessages.USE_POINTS_REQUIRED)
        @Min(value = PaymentValidationConstants.USE_POINTS_MIN, message = PaymentValidationMessages.USE_POINTS_MIN)
        @Max(value = PaymentValidationConstants.USE_POINTS_MAX, message = PaymentValidationMessages.USE_POINTS_MAX)
        Integer usePoints,

        @NotBlank(message = PaymentValidationMessages.PHONE_REQUIRED)
        @Size(max = PaymentValidationConstants.PHONE_MAX_LENGTH, message = PaymentValidationMessages.PHONE_MAX_LENGTH)
        String phone,

        @NotBlank(message = PaymentValidationMessages.ZONECODE_REQUIRED)
        @Size(min = PaymentValidationConstants.ZONECODE_LENGTH, max = PaymentValidationConstants.ZONECODE_LENGTH, message = PaymentValidationMessages.ZONECODE_LENGTH)
        String zonecode,

        @NotBlank(message = PaymentValidationMessages.ADDRESS1_REQUIRED)
        @Size(max = PaymentValidationConstants.ADDRESS_MAX_LENGTH, message = PaymentValidationMessages.ADDRESS1_MAX_LENGTH)
        String address1,

        @Size(max = PaymentValidationConstants.ADDRESS_MAX_LENGTH, message = PaymentValidationMessages.ADDRESS2_MAX_LENGTH)
        String address2
) {
}