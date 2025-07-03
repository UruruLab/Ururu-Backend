package com.ururulab.ururu.order.dto.request;

import com.ururulab.ururu.order.dto.validation.OrderValidationConstants;
import com.ururulab.ururu.order.dto.validation.OrderValidationMessages;
import jakarta.validation.constraints.*;

/**
 * 주문 아이템 요청 DTO (공구 내 개별 옵션)
 */
public record OrderItemRequest(
        @NotNull(message = OrderValidationMessages.GROUPBUY_OPTION_ID_REQUIRED)
        Long groupbuyOptionId,

        @NotNull(message = OrderValidationMessages.QUANTITY_REQUIRED)
        @Min(value = OrderValidationConstants.QUANTITY_MIN, message = OrderValidationMessages.QUANTITY_MIN)
        @Max(value = OrderValidationConstants.QUANTITY_MAX, message = OrderValidationMessages.QUANTITY_MAX)
        Integer quantity
) {
}