package com.ururulab.ururu.groupBuy.event;

import java.util.List;

public record GroupBuyDetailImageDeleteEvent(
        Long groupBuyId,
        List<String> imageUrls
) {
}
