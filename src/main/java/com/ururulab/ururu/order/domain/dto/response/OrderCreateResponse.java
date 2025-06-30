package com.ururulab.ururu.order.domain.dto.response;

import java.util.List;

/**
 * 주문서 생성 응답 DTO
 * POST /api/groupbuys/{groupbuyId}/orders, POST /api/cart/orders 공통 응답
 */
public record OrderCreateResponse(
        String orderId,
        List<OrderItemResponse> orderItems,
        Integer totalAmount,           // 상품 총 금액
        Integer availablePoints,       // 사용 가능한 포인트
        Integer shippingFee
) {
}
