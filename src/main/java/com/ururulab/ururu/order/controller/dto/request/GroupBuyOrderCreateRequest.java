package com.ururulab.ururu.order.controller.dto.request;

import com.ururulab.ururu.order.controller.dto.validation.OrderValidationConstants;
import com.ururulab.ururu.order.controller.dto.validation.OrderValidationMessages;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 공구 주문서 생성 요청 DTO
 * POST /api/groupbuys/{groupbuyId}/orders
 */
public record GroupBuyOrderCreateRequest(
        @NotEmpty(message = OrderValidationMessages.ORDER_ITEMS_REQUIRED)
        @Size(max = OrderValidationConstants.ORDER_ITEMS_MAX, message = OrderValidationMessages.ORDER_ITEMS_MAX)
        @Valid
        List<OrderItemRequest> orderItems
) {
}