package com.ururulab.ururu.product.domain.dto.request;

import com.ururulab.ururu.product.domain.entity.Product;
import com.ururulab.ururu.product.domain.entity.ProductNotice;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import static com.ururulab.ururu.product.domain.dto.validation.ProductValidationConstants.*;
import static com.ururulab.ururu.product.domain.dto.validation.ProductValidationMessages.*;

public record ProductNoticeRequest(
        @NotBlank(message = CAPACITY_REQUIRED)
        @Size(max = CAPACITY_MAX, message = CAPACITY_SIZE)
        String capacity,

        @NotBlank(message = SPEC_REQUIRED)
        @Size(max = SPEC_MAX, message = SPEC_SIZE)
        String spec,

        @NotBlank(message = EXPIRY_REQUIRED)
        @Size(max = EXPIRY_MAX, message = EXPIRY_SIZE)
        String expiry,

        @NotBlank(message = USAGE_REQUIRED)
        String usage,

        @NotBlank(message = MANUFACTURER_REQUIRED)
        @Size(max = MANUFACTURER_MAX, message = MANUFACTURER_SIZE)
        String manufacturer,

        @NotBlank(message = RESPONSIBLE_SELLER_REQUIRED)
        @Size(max = RESPONSIBLE_SELLER_MAX, message = RESPONSIBLE_SELLER_SIZE)
        String responsibleSeller,

        @NotBlank(message = COUNTRY_OF_ORIGIN_REQUIRED)
        @Size(max = COUNTRY_OF_ORIGIN_MAX, message = COUNTRY_OF_ORIGIN_SIZE)
        String countryOfOrigin,

        @NotNull(message = FUNCTIONAL_COSMETICS_REQUIRED)
        Boolean functionalCosmetics,

        @NotBlank(message = CAUTION_REQUIRED)
        String caution,

        @NotBlank(message = WARRANTY_REQUIRED)
        String warranty,

        @NotBlank(message = CUSTOMER_SERVICE_NUMBER_REQUIRED)
        @Size(max = CUSTOMER_SERVICE_NUMBER_MAX, message = CUSTOMER_SERVICE_NUMBER_SIZE)
        String customerServiceNumber
) {
    public ProductNotice toEntity(Product product) {
        return ProductNotice.of(
                product,
                capacity,
                spec,
                expiry,
                usage,
                manufacturer,
                responsibleSeller,
                countryOfOrigin,
                functionalCosmetics,
                caution,
                warranty,
                customerServiceNumber
        );
    }
}
