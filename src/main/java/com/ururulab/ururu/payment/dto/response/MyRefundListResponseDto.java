package com.ururulab.ururu.payment.dto.response;

import java.util.List;

public record MyRefundListResponseDto(
        List<MyRefundResponseDto> refunds,
        Integer page,
        Integer size,
        Long total
) {
}