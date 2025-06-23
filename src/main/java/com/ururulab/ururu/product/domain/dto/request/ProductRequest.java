package com.ururulab.ururu.product.domain.dto.request;

import com.ururulab.ururu.product.domain.entity.Product;
import com.ururulab.ururu.product.domain.entity.enumerated.Status;

import java.util.List;

public record ProductRequest(
        String name,
        String description,
        List<Long> categoryIds,
        List<ProductOptionRequest> productOptions,
        ProductNoticeRequest productNotice
) {
    public Product toEntity() {
        return Product.of(
                name,
                description,
                Status.ACTIVE
        );
    }
}
