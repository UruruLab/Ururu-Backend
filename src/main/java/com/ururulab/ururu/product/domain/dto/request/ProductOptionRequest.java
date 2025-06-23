package com.ururulab.ururu.product.domain.dto.request;

import com.ururulab.ururu.product.domain.entity.Product;
import com.ururulab.ururu.product.domain.entity.ProductOption;

public record ProductOptionRequest(
        Long id, // Update시에만 사용 (Create시에는 null)
        String name,
        Integer price,
        String imageUrl,
        String fullIngredients
) {
    public ProductOption toEntity(Product product) {
        return ProductOption.of(
                product,
                name,
                price,
                imageUrl,
                fullIngredients
        );
    }
}
