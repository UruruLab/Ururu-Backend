package com.ururulab.ururu.order.controller.dto.request;

import com.ururulab.ururu.order.controller.dto.validation.CartValidationConstants;
import com.ururulab.ururu.order.controller.dto.validation.CartValidationMessages;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartItemQuantityChangeRequest(
        @NotNull(message = CartValidationMessages.QUANTITY_CHANGE_REQUIRED)
        @Min(value = CartValidationConstants.QUANTITY_CHANGE_MIN, message = CartValidationMessages.QUANTITY_CHANGE_MIN)
        @Max(value = CartValidationConstants.QUANTITY_CHANGE_MAX, message = CartValidationMessages.QUANTITY_CHANGE_MAX)
        Integer quantityChange
) {
}