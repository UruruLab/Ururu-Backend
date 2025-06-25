package com.ururulab.ururu.product.domain.dto.request;

import com.ururulab.ururu.product.domain.entity.Product;
import com.ururulab.ururu.product.domain.entity.enumerated.Status;
import com.ururulab.ururu.seller.domain.entity.Seller;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

import static com.ururulab.ururu.product.domain.dto.validation.ProductValidationConstants.*;
import static com.ururulab.ururu.product.domain.dto.validation.ProductValidationMessages.*;

public record ProductRequest(
        @NotBlank(message = PRODUCT_NAME_REQUIRED)
        @Size(max = PRODUCT_NAME_MAX, message = PRODUCT_NAME_SIZE)
        String name,

        @NotBlank(message = PRODUCT_DESCRIPTION_REQUIRED)
        @Size(max = PRODUCT_DESCRIPTION_MAX, message = PRODUCT_DESCRIPTION_SIZE)
        String description,

        @NotEmpty(message = CATEGORIES_REQUIRED)
        List<Long> categoryIds,

        @NotEmpty(message = PRODUCT_OPTIONS_REQUIRED)
        List<@Valid ProductOptionRequest> productOptions,

        @Valid
        @NotNull(message = PRODUCT_NOTICE_REQUIRED)
        ProductNoticeRequest productNotice
) {
    public Product toEntity(
            Seller seller
    ) {
        return Product.of(
                seller,
                name,
                description,
                Status.ACTIVE
        );
    }
}
