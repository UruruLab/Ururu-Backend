package com.ururulab.ururu.groupBuy.dto.response;

import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyOption;
import com.ururulab.ururu.groupBuy.domain.entity.enumerated.GroupBuyStatus;

import java.time.Instant;
import java.util.List;

public record GroupBuySellerListResponse(
        Long id,
        String title,
        String thumbnailUrl,
        Integer displayFinalPrice,
        Integer startPrice,
        Integer maxDiscountRate,
        GroupBuyStatus status,
        Instant startAt,
        Instant endsAt,
        Integer totalStock,
        Integer soldQuantity,
        Integer orderCount,
        Instant createdAt,
        Instant updatedAt
) {
    public static GroupBuySellerListResponse from(GroupBuy groupBuy,
                                                  List<GroupBuyOption> options,
                                                  Integer orderCount) {

        Integer startPrice = options.stream()
                .map(GroupBuyOption::getPriceOverride)
                .min(Integer::compareTo)
                .orElse(0);

        Integer totalStock = options.stream()
                .mapToInt(GroupBuyOption::getInitialStock)
                .sum();

        Integer soldQuantity = options.stream()
                .mapToInt(GroupBuyOption::getSoldQuantity)
                .sum();

        return new GroupBuySellerListResponse(
                groupBuy.getId(),
                groupBuy.getTitle(),
                groupBuy.getThumbnailUrl(),
                groupBuy.getDisplayFinalPrice(),
                startPrice,
                groupBuy.getMaxDiscountRate(),
                groupBuy.getStatus(),
                groupBuy.getStartAt(),
                groupBuy.getEndsAt(),
                totalStock,
                soldQuantity,
                orderCount,
                groupBuy.getCreatedAt(),
                groupBuy.getUpdatedAt()
        );
    }
}
