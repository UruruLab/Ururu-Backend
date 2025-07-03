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

    //임시 URL 경로
    private static final String DEFAULT_IMAGE_URL = "/images/default-product-option.jpg";

    public static ProductOptionResponse from(ProductOption productOption) {
        String imageUrl = (productOption.getImageUrl() != null && !productOption.getImageUrl().trim().isEmpty())
                ? productOption.getImageUrl()
                : DEFAULT_IMAGE_URL;

        return new ProductOptionResponse(
                productOption.getId(),
                productOption.getName(),
                productOption.getPrice(),
                imageUrl,
                productOption.getFullIngredients(),
                productOption.getCreatedAt(),
                productOption.getUpdatedAt()
        );
    }
}
