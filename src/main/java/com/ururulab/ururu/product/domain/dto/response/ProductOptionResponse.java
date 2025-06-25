package com.ururulab.ururu.product.domain.dto.response;

import com.ururulab.ururu.product.domain.entity.ProductOption;

import java.time.ZonedDateTime;

public record ProductOptionResponse(
        Long id,
        String name,
        Integer price,
        String imageUrl,
        String fullIngredients,
        ZonedDateTime createdAt,
        ZonedDateTime updatedAt
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
