package com.ururulab.ururu.order.domain.dto.request;

import com.ururulab.ururu.order.domain.dto.validation.ValidationConstants;
import com.ururulab.ururu.order.domain.dto.validation.ValidationMessages;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartItemAddRequest(
        @NotNull(message = ValidationMessages.GROUPBUY_OPTION_ID_REQUIRED)
        Long groupbuyOptionId,

        @NotNull(message = ValidationMessages.QUANTITY_REQUIRED)
        @Min(value = ValidationConstants.QUANTITY_MIN, message = ValidationMessages.QUANTITY_MIN)
        @Max(value = ValidationConstants.QUANTITY_MAX, message = ValidationMessages.QUANTITY_MAX)
        Integer quantity
) {
}
