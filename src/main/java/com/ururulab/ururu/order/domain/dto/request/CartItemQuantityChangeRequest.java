package com.ururulab.ururu.order.domain.dto.request;

import com.ururulab.ururu.order.domain.dto.validation.ValidationConstants;
import com.ururulab.ururu.order.domain.dto.validation.ValidationMessages;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartItemQuantityChangeRequest(
        @NotNull(message = ValidationMessages.QUANTITY_CHANGE_REQUIRED)
        @Min(value = ValidationConstants.QUANTITY_CHANGE_MIN, message = ValidationMessages.QUANTITY_CHANGE_MIN)
        @Max(value = ValidationConstants.QUANTITY_CHANGE_MAX, message = ValidationMessages.QUANTITY_CHANGE_MAX)
        Integer quantityChange
) {
}