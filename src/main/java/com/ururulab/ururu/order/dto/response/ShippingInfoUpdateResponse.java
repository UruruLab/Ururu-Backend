package com.ururulab.ururu.order.dto.response;

public record ShippingInfoUpdateResponse(
        String orderId,
        String trackingNumber
) {
}