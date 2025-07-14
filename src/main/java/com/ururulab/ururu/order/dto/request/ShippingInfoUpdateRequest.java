package com.ururulab.ururu.order.dto.request;

import com.ururulab.ururu.order.dto.validation.ShippingValidationConstants;
import com.ururulab.ururu.order.dto.validation.ShippingValidationMessages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ShippingInfoUpdateRequest(
        @NotBlank(message = ShippingValidationMessages.TRACKING_NUMBER_REQUIRED)
        @Size(max = ShippingValidationConstants.TRACKING_NUMBER_MAX_LENGTH, message = ShippingValidationMessages.TRACKING_NUMBER_MAX_LENGTH)
        String trackingNumber
) {
}
