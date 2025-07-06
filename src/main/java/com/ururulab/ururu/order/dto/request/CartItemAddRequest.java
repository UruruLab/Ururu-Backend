package com.ururulab.ururu.order.dto.request;

import com.ururulab.ururu.order.dto.validation.CartValidationConstants;
import com.ururulab.ururu.order.dto.validation.CartValidationMessages;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartItemAddRequest(
        @NotNull(message = CartValidationMessages.GROUPBUY_OPTION_ID_REQUIRED)
        Long groupbuyOptionId,

        @NotNull(message = CartValidationMessages.QUANTITY_REQUIRED)
        @Min(value = CartValidationConstants.QUANTITY_MIN, message = CartValidationMessages.QUANTITY_MIN)
        @Max(value = CartValidationConstants.QUANTITY_MAX, message = CartValidationMessages.QUANTITY_MAX)
        Integer quantity
) {
}
