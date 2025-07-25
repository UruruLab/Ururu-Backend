package com.ururulab.ururu.groupBuy.dto.common;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import static com.ururulab.ururu.groupBuy.dto.validation.GroupBuyValidationConstants.*;

public record DiscountStageDto(

        @JsonProperty("count")
        @JsonAlias({"minQuantity", "count"})
        @NotNull(message = MIN_QUANTITY_REQUIRED_MSG)
        @Min(value = LIMIT_QUANTITY_MIN, message = MIN_QUANTITY_MIN)
        Integer minQuantity,

        @JsonProperty("rate")
        @JsonAlias({"discountRate", "rate"})
        @NotNull(message = DISCOUNT_RATE_REQUIRED_MSG)
        @Min(value = LIMIT_RATE_MIN, message = DISCOUNT_RATE_MIN)
        @Max(value = LIMIT_RATE_MAX, message = DISCOUNT_RATE_MAX)
        Integer discountRate
) {
    public static DiscountStageDto of(Integer minQuantity, Integer discountRate) {
        return new DiscountStageDto(minQuantity, discountRate);
    }
}
