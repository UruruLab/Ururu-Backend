package com.ururulab.ururu.groupBuy.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
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
        Integer orderCount, // 주문량 (정렬 기준)
        Instant createdAt
) {
    public static GroupBuyListResponse from(GroupBuy groupBuy,
                                            List<GroupBuyOption> options,
                                            Integer orderCount) {

        List<DiscountStageDto> stages = DiscountStageParser.parseDiscountStages(groupBuy.getDiscountStages());
        Integer maxDiscountRate = stages.get(stages.size() - 1).discountRate();
        Long remainingSeconds = TimeCalculator.calculateRemainingSeconds(groupBuy.getEndsAt());

        return new GroupBuyListResponse
                (
                groupBuy.getId(), // 공구 아이디
                groupBuy.getTitle(), // 공구 제목
                groupBuy.getThumbnailUrl(), //공구 썸네일
                groupBuy.getDisplayFinalPrice(), // 공구 메인 가격
                options.get(0).getPriceOverride(), // 공구 첫번째 공구 시작가
                maxDiscountRate, // 최대 할인률
                groupBuy.getEndsAt(), // 공구 종료일
                remainingSeconds, // 종료일까지 남은 초
                orderCount, // 주문량
                groupBuy.getCreatedAt() // 생성일
        );
    }
}
