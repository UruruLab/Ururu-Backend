package com.ururulab.ururu.product.dto.response;

import com.ururulab.ururu.product.domain.entity.ProductOption;

import java.time.Instant;

public record ProductOptionResponse(
        Long id,
        String name,
        Integer price,
        String imageUrl,
        String fullIngredients,
        Instant createdAt,
        Instant updatedAt
) {

    public static ProductOptionResponse from(ProductOption productOption) {
        return new ProductOptionResponse(
                productOption.getId(),
                productOption.getName(),
                productOption.getPrice(),
                productOption.getImageUrl(),
                productOption.getFullIngredients(),
                productOption.getCreatedAt(),
                productOption.getUpdatedAt()
        );
    }
}
