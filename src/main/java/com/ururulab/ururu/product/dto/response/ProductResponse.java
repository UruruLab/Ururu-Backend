package com.ururulab.ururu.product.dto.response;

import com.ururulab.ururu.product.domain.entity.Product;
import com.ururulab.ururu.product.domain.entity.enumerated.Status;

import java.time.Instant;
import java.util.List;

public record ProductResponse(
        Long id,
        String name,
        String description,
        Status status,
        Instant createdAt,
        Instant updatedAt,
        List<CategoryResponse> categories,
        List<ProductOptionResponse> productOptions,
        ProductNoticeResponse productNotice,
        List<ProductTagResponse> productTags
) {
    public static ProductResponse from(
            Product product,
            List<CategoryResponse> categoryResponses,
            List<ProductOptionResponse> productOptionResponses,
            ProductNoticeResponse productNoticeResponse,
            List<ProductTagResponse> productTagsResponses
    ) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getStatus(),
                product.getCreatedAt(),
                product.getUpdatedAt(),
                categoryResponses,
                productOptionResponses,
                productNoticeResponse,
                productTagsResponses
        );
    }
}
