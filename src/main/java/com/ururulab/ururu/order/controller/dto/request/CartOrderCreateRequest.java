package com.ururulab.ururu.order.controller.dto.request;

import com.ururulab.ururu.order.controller.dto.validation.OrderValidationConstants;
import com.ururulab.ururu.order.controller.dto.validation.OrderValidationMessages;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 장바구니 주문서 생성 요청 DTO
 * POST /api/cart/orders
 */
public record CartOrderCreateRequest(
        @NotEmpty(message = OrderValidationMessages.CART_ITEM_IDS_REQUIRED)
        @Size(max = OrderValidationConstants.CART_ITEMS_MAX, message = OrderValidationMessages.CART_ITEM_IDS_MAX)
        List<Long> cartItemIds
) {
}