package com.ururulab.ururu.groupBuy.dto.response;

import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyOption;

import java.util.Map;

public record GroupBuyOptionInfoResponse(
        Long id,
        Long productOptionId,
        String optionName,
        String optionImageUrl,
        String fullIngredients,  // 전성분 추가
        Integer stock, // 기존 재고
        Integer currentStock, // 현재 남은 재고
        Boolean isOutOfStock,    // 품절 여부
        Integer priceOverride,
        Integer salePrice
) {
    public static GroupBuyOptionInfoResponse from(GroupBuyOption option, Map<Long, Integer> currentStocks) {
        Integer currentStock = currentStocks.getOrDefault(option.getId(), option.getStock());
        return new GroupBuyOptionInfoResponse(
                option.getId(),
                option.getProductOption().getId(),
                option.getProductOption().getName(),
                option.getProductOption().getImageUrl(),
                option.getProductOption().getFullIngredients(), // 전성분
                option.getStock(),
                currentStock,
                currentStock <= 0,
                option.getPriceOverride(),
                option.getSalePrice()
        );
    }
}
