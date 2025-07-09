package com.ururulab.ururu.groupBuy.event;

import java.util.List;

public record GroupBuyThumbnailDeleteEvent(
        Long groupBuyId,
        List<String> imageUrls
) {
}
