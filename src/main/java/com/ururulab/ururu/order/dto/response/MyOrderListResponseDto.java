package com.ururulab.ururu.order.dto.response;

import java.util.List;

public record MyOrderListResponseDto(
        Integer inProgress,
        Integer confirmed,
        Integer refundPending,
        List<MyOrderResponseDto> orders,
        Integer page,
        Integer size,
        Long total
) {
}
