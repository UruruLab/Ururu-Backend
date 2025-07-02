package com.ururulab.ururu.groupBuy.event;

public record GroupBuyThumbnailDeleteEvent(
        Long groupBuyId,
        String imageUrl
) {
}
