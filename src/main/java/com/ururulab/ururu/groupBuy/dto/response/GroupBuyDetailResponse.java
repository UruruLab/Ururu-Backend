package com.ururulab.ururu.groupBuy.dto.response;

import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyImage;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyOption;
import com.ururulab.ururu.groupBuy.domain.entity.enumerated.GroupBuyStatus;
import com.ururulab.ururu.groupBuy.dto.common.DiscountStageDto;
import com.ururulab.ururu.groupBuy.util.DiscountStageParser;
import com.ururulab.ururu.groupBuy.util.TimeCalculator;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record GroupBuyDetailResponse(
        Long id,
        String title,
        String description,
        String thumbnailUrl,
        Integer displayFinalPrice,
        List<DiscountStageDto> discountStages,
        Integer limitQuantityPerMember,
        GroupBuyStatus status,
        Instant endsAt,
        Long remainingTimeSeconds, // 남은 시간 (초 단위)
        Integer currentOrderCount,

        // 상품 정보
        ProductInfoResponse product,

        // 옵션 정보
        List<GroupBuyOptionInfoResponse> options,

        // 이미지 정보
        List<GroupBuyImageInfoResponse> images,

        // 메타 정보
        Instant createdAt,
        Instant updatedAt
) {
    public static GroupBuyDetailResponse from(GroupBuy groupBuy,
                                              List<GroupBuyOption> options,
                                              List<GroupBuyImage> images,
                                              Map<Long, Integer> currentStocks,
                                              Integer currentOrderCount
    ) {

        List<DiscountStageDto> parsedStages = DiscountStageParser.parseDiscountStages(groupBuy.getDiscountStages());
        Long remainingSeconds = TimeCalculator.calculateRemainingSeconds(groupBuy.getEndsAt());

        return new GroupBuyDetailResponse(
                groupBuy.getId(),
                groupBuy.getTitle(),
                groupBuy.getDescription(),
                groupBuy.getThumbnailUrl(),
                groupBuy.getDisplayFinalPrice(),
                parsedStages,
                groupBuy.getLimitQuantityPerMember(),
                groupBuy.getStatus(),
                groupBuy.getEndsAt(),
                remainingSeconds,
                currentOrderCount,

                ProductInfoResponse.from(groupBuy.getProduct()),

                options.stream()
                        .map(option -> GroupBuyOptionInfoResponse.from(option, currentStocks))
                        .toList(),

                images.stream()
                        .filter(image -> !image.getIsDeleted())
                        .map(GroupBuyImageInfoResponse::from)
                        .toList(),

                groupBuy.getCreatedAt(),
                groupBuy.getUpdatedAt()
        );
    }
}
