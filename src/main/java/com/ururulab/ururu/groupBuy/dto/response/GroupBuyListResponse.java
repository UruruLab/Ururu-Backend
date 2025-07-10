package com.ururulab.ururu.groupBuy.dto.response;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyOption;
import com.ururulab.ururu.groupBuy.dto.common.DiscountStageDto;
import com.ururulab.ururu.groupBuy.util.DiscountStageParser;

import java.time.Instant;
import java.util.List;

import static com.ururulab.ururu.global.exception.error.ErrorCode.GROUPBUY_NO_OPTIONS;

public record GroupBuyListResponse(
        Long id,
        String title,
        String thumbnailUrl,
        Integer displayFinalPrice,
        Integer startPrice, // 첫번째 옵션의 공구시작가
        Integer maxDiscountRate, // 리워드 최고 할인률
        Instant endsAt,
        Integer orderCount, // 주문량 (정렬 기준)
        Instant createdAt
) {
    public static GroupBuyListResponse from(GroupBuy groupBuy,
                                            List<GroupBuyOption> options,
                                            Integer orderCount) {

        List<DiscountStageDto> stages = DiscountStageParser.parseDiscountStages(groupBuy.getDiscountStages());
        Integer maxDiscountRate = stages.get(stages.size() - 1).discountRate();
        Integer startPrice = options.stream()
                .map(GroupBuyOption::getPriceOverride)
                .min(Integer::compareTo)
                .orElseThrow(() -> new BusinessException(GROUPBUY_NO_OPTIONS));


        return new GroupBuyListResponse
                (
                groupBuy.getId(), // 공구 아이디
                groupBuy.getTitle(), // 공구 제목
                groupBuy.getThumbnailUrl(), //공구 썸네일
                groupBuy.getDisplayFinalPrice(), // 공구 메인 가격
                startPrice, // 공구 옵션 중 최저가
                maxDiscountRate, // 최대 할인률
                groupBuy.getEndsAt(), // 공구 종료일
                orderCount, // 주문량
                groupBuy.getCreatedAt() // 생성일
        );
    }
}
