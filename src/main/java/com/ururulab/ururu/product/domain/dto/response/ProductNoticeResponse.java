package com.ururulab.ururu.product.domain.dto.response;

import com.ururulab.ururu.product.domain.entity.ProductNotice;

public record ProductNoticeResponse(
        Long id,
        String capacity,
        String spec,
        String expiry,
        String usage,
        String manufacturer,
        String responsibleSeller,
        String countryOfOrigin,
        boolean functionalCosmetics,
        String caution,
        String warranty,
        String customerServiceNumber
) {
    public static ProductNoticeResponse from(ProductNotice productNotice) {
        return new ProductNoticeResponse(
                productNotice.getId(),
                productNotice.getCapacity(),
                productNotice.getSpec(),
                productNotice.getExpiry(),
                productNotice.getUsage(),
                productNotice.getManufacturer(),
                productNotice.getResponsibleSeller(),
                productNotice.getCountryOfOrigin(),
                productNotice.isFunctionalCosmetics(),
                productNotice.getCaution(),
                productNotice.getWarranty(),
                productNotice.getCustomerServiceNumber()
        );
    }
}
