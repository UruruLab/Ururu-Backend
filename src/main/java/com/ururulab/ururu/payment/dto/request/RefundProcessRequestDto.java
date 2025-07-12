package com.ururulab.ururu.payment.dto.request;

import com.ururulab.ururu.payment.dto.validation.RefundValidationConstants;
import com.ururulab.ururu.payment.dto.validation.RefundValidationMessages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RefundProcessRequestDto(
        @NotBlank(message = RefundValidationMessages.ACTION_REQUIRED)
        @Pattern(regexp = RefundValidationConstants.REFUND_ACTION_PATTERN, message = RefundValidationMessages.ACTION_INVALID)
        String action,

        @Size(max = RefundValidationConstants.REJECT_REASON_MAX_LENGTH, message = RefundValidationMessages.REJECT_REASON_MAX_LENGTH)
        String rejectReason
) {
}