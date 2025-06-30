package com.ururulab.ururu.product.domain.dto.response;

import com.ururulab.ururu.product.domain.entity.Product;
import com.ururulab.ururu.product.domain.entity.enumerated.Status;

import java.time.Instant;
import java.util.List;

public record ProductListResponse(
        Long id,
        String name,
        String description,
        Status status,
        Instant createdAt,
        Instant updatedAt,
        List<CategoryResponse> categories,
        List<ProductTagResponse> tagCategories
) {
    public static ProductListResponse from(Product product, List<CategoryResponse> categories, List<ProductTagResponse> tagCategories) {
        return new ProductListResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getStatus(),
                product.getCreatedAt(),
                product.getUpdatedAt(),
                categories,
                tagCategories
        );
    }
}
