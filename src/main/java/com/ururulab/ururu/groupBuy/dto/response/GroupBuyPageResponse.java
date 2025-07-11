package com.ururulab.ururu.groupBuy.dto.response;

import java.util.List;

public record GroupBuyPageResponse(
        List<GroupBuyListResponse> items,
        String nextCursor,
        boolean hasMore
) {
    public static GroupBuyPageResponse of(List<GroupBuyListResponse> items, String nextCursor, boolean hasMore) {
        return new GroupBuyPageResponse(items, nextCursor, hasMore);
    }
}
