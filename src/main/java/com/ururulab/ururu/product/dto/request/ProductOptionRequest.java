package com.ururulab.ururu.product.dto.request;

import com.ururulab.ururu.product.domain.entity.Product;
import com.ururulab.ururu.product.domain.entity.ProductOption;
import io.micrometer.common.lang.Nullable;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import static com.ururulab.ururu.product.dto.validation.ProductValidationConstants.*;
import static com.ururulab.ururu.product.dto.validation.ProductValidationMessages.*;

public record ProductOptionRequest(
        Long id, // Update시에만 사용 (Create시에는 null)

        @NotBlank(message = OPTION_NAME_REQUIRED)
        @Size(max = PRODUCT_OPTION_NAME_MAX, message = OPTION_NAME_SIZE)
        String name,

        @NotNull(message = OPTION_PRICE_REQUIRED)
        @Min(value = 0, message = OPTION_PRICE_MIN)
        Integer price,

        @Nullable
        String imageUrl,

        @NotBlank(message = FULL_INGREDIENTS_REQUIRED)
        @Size(max = FULL_INGREDIENTS_MAX, message = FULL_INGREDIENTS_SIZE)
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
