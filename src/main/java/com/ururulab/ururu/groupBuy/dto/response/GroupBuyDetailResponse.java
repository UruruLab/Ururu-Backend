package com.ururulab.ururu.groupBuy.dto.response;

import com.ururulab.ururu.global.exception.BusinessException;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyImage;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyOption;
import com.ururulab.ururu.groupBuy.domain.entity.enumerated.GroupBuyStatus;
import com.ururulab.ururu.groupBuy.dto.common.DiscountStageDto;
import com.ururulab.ururu.groupBuy.util.DiscountStageParser;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static com.ururulab.ururu.global.exception.error.ErrorCode.GROUPBUY_NO_OPTIONS;

public record GroupBuyDetailResponse(
        Long id,
        String title,
        String description,
        String thumbnailUrl,
        Integer displayFinalPrice,
        Integer startPrice,         // 공동구매 시작 가격
        Integer maxDiscountRate,     // 최대 할인율
        List<DiscountStageDto> discountStages,
        Integer limitQuantityPerMember,
        GroupBuyStatus status,
        Instant endsAt,
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
        Integer startPrice = options.stream()
                .map(GroupBuyOption::getPriceOverride)
                .min(Integer::compareTo)
                .orElseThrow(() -> new BusinessException(GROUPBUY_NO_OPTIONS));

        return new GroupBuyDetailResponse(
                groupBuy.getId(),
                groupBuy.getTitle(),
                groupBuy.getDescription(),
                groupBuy.getThumbnailUrl(),
                groupBuy.getDisplayFinalPrice(),
                startPrice,
                groupBuy.getMaxDiscountRate(),
                parsedStages,
                groupBuy.getLimitQuantityPerMember(),
                groupBuy.getStatus(),
                groupBuy.getEndsAt(),
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
