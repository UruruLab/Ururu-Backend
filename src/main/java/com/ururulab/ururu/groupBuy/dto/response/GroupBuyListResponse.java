package com.ururulab.ururu.groupBuy.dto.response;

import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyOption;
import com.ururulab.ururu.groupBuy.dto.common.DiscountStageDto;
import com.ururulab.ururu.groupBuy.util.DiscountStageParser;
import com.ururulab.ururu.groupBuy.util.TimeCalculator;

import java.time.Instant;
import java.util.List;

public record GroupBuyListResponse(
        Long id,
        String title,
        String thumbnailUrl,
        Integer displayFinalPrice,
        Integer startPrice, // 첫번째 옵션의 공구시작가
        Integer maxDiscountRate, // 리워드 최고 할인률
        Instant endsAt,
        Long remainingTimeSeconds, // 남은 시간 (초 단위)
        Integer orderCount // 주문량 (정렬 기준)
) {
    public static GroupBuyListResponse from(GroupBuy groupBuy,
                                            List<GroupBuyOption> options,
                                            Integer orderCount) {

        List<DiscountStageDto> stages = DiscountStageParser.parseDiscountStages(groupBuy.getDiscountStages());
        Long remainingSeconds = TimeCalculator.calculateRemainingSeconds(groupBuy.getEndsAt());

        return new GroupBuyListResponse(
                groupBuy.getId(),
                groupBuy.getTitle(),
                groupBuy.getThumbnailUrl(),
                groupBuy.getDisplayFinalPrice(),
                options.get(0).getPriceOverride(),
                stages.get(stages.size() - 1).discountRate(),
                groupBuy.getEndsAt(),
                remainingSeconds,
                orderCount
        );
    }
}
