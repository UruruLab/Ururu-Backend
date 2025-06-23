package com.ururulab.ururu.product.domain.dto.request;

import com.ururulab.ururu.product.domain.entity.Product;
import com.ururulab.ururu.product.domain.entity.ProductNotice;

public record ProductNoticeRequest(
        String capacity,
        String spec,
        String expiry,
        String usage,
        String manufacturer,
        String responsibleSeller,
        String countryOfOrigin,
        Boolean functionalCosmetics,
        String caution,
        String warranty,
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
