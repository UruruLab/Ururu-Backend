package com.ururulab.ururu.product.event;

import java.util.List;

public record ProductImageDeleteEvent(
        Long productId,
        List<String> imageUrls
) {
}
