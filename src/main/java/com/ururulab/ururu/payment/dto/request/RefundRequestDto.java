package com.ururulab.ururu.payment.dto.request;

import com.ururulab.ururu.global.validation.EnumValue;
import com.ururulab.ururu.payment.domain.entity.enumerated.RefundType;
import com.ururulab.ururu.payment.dto.validation.RefundValidationConstants;
import com.ururulab.ururu.payment.dto.validation.RefundValidationMessages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefundRequestDto(
        @NotBlank(message = RefundValidationMessages.TYPE_REQUIRED)
        @EnumValue(enumClass = RefundType.class, message = RefundValidationMessages.TYPE_INVALID)
        String type,

        @NotBlank(message = RefundValidationMessages.REASON_REQUIRED)
        @Size(max = RefundValidationConstants.REASON_MAX_LENGTH, message = RefundValidationMessages.REASON_MAX_LENGTH)
        String reason,

        @Size(max = RefundValidationConstants.RETURN_TRACKING_NUMBER_MAX_LENGTH, message = RefundValidationMessages.RETURN_TRACKING_NUMBER_TOO_LONG)
        String returnTrackingNumber  //
) {
}
