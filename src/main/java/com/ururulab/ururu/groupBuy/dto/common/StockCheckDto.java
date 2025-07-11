package com.ururulab.ururu.groupBuy.dto.common;

public record StockCheckDto(
        Long optionId,
        Integer stock,
        Long groupBuyId
) {
}
