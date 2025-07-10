package com.ururulab.ururu.groupBuy.dto.common;

import com.ururulab.ururu.groupBuy.dto.response.GroupBuyListResponse;

import java.time.Instant;

public record CursorInfoDto(
        Long id,
        Instant createdAt,
        Instant endsAt,
        Integer price,
        Integer orderCount
) {
    public static CursorInfoDto from(GroupBuyListResponse response) {
        return new CursorInfoDto(
                response.id(),
                response.createdAt(),
                response.endsAt(),
                response.displayFinalPrice(),
                response.orderCount()
        );
    }
}
