package com.ururulab.ururu.groupBuy.controller.dto.request;

import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyOption;
import com.ururulab.ururu.product.domain.entity.ProductOption;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import static com.ururulab.ururu.groupBuy.controller.dto.validation.GroupBuyValidationMessages.*;

public record GroupBuyOptionRequest(
        Long id, // Update시에만 사용 (Create시에는 null)

        @NotNull(message = PRODUCT_OPTION_ID_REQUIRED)
        Long productOptionId,

        @NotNull(message = STOCK_REQUIRED)
        @Min(value = 0, message = STOCK_MIN)
        Integer stock,

        @NotNull(message = PRICE_OVERRIDE_REQUIRED)
        @Min(value = 0, message = PRICE_OVERRIDE_MIN)
        Integer priceOverride
) {
    public GroupBuyOption toEntity(GroupBuy groupBuy, ProductOption productOption) {
        return GroupBuyOption.of(
                groupBuy,
                productOption,
                stock,
                priceOverride,
                priceOverride
        );
    }
}
