package com.ururulab.ururu.groupBuy.dto.response;

import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyOption;

import java.util.Map;

public record GroupBuyOptionInfoResponse(
        Long id,
        Long productOptionId,
        String optionName,
        String optionImageUrl,
        String fullIngredients,  // 전성분 추가
        Integer initialStock,    // 초기 재고 (변경되지 않음)
        Integer currentStock, // 기존 재고
        Integer soldQuantity,    // 판매량 (initialStock - currentStock)
        Boolean isOutOfStock,    // 품절 여부
        Integer priceOverride,
        Integer salePrice
) {
    public static GroupBuyOptionInfoResponse from(GroupBuyOption option, Map<Long, Integer> currentStocks) {
        Integer soldQuantity = option.getSoldQuantity(); // initialStock - stock
        Integer currentStock = option.getStock();

        return new GroupBuyOptionInfoResponse(
                option.getId(),
                option.getProductOption().getId(),
                option.getProductOption().getName(),
                option.getProductOption().getImageUrl(),
                option.getProductOption().getFullIngredients(), // 전성분
                option.getInitialStock(), // 초기 재고
                currentStock, // 현재 재고
                soldQuantity, // 판매량
                currentStock <= 0, // 품절 여부
                option.getPriceOverride(),
                option.getSalePrice()
        );
    }
}
