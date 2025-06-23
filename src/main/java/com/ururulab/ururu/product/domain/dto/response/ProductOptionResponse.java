package com.ururulab.ururu.product.domain.dto.response;

import com.ururulab.ururu.product.domain.entity.ProductOption;

public record ProductOptionResponse(
        Long id,
        String name,
        int price,
        String imageUrl,
        String fullIngredients
) {
    public static ProductOptionResponse from(ProductOption productOption) {
        return new ProductOptionResponse(
                productOption.getId(),
                productOption.getName(),
                productOption.getPrice(),
                productOption.getImageUrl(),
                productOption.getFullIngredients()
        );
    }
}
