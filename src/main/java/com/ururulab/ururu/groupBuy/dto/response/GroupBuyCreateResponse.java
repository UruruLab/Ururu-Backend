package com.ururulab.ururu.groupBuy.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ururulab.ururu.groupBuy.dto.common.DiscountStageDto;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.entity.enumerated.GroupBuyStatus;
import com.ururulab.ururu.groupBuy.util.DiscountStageParser;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GroupBuyCreateResponse(
        Long id,
        String title,
        String description,
        String thumbnailUrl, // null이면 기본 이미지로 처리됨
        List<DiscountStageDto> discountStages, // JSON 형태의 할인 단계 정보
        Integer limitQuantityPerMember,
        GroupBuyStatus status,
        Instant startAt,
        Instant endsAt,
        Instant createdAt,
        Instant updatedAt,
        Long productId,
        String productName
) {
    private static final String DEFAULT_THUMBNAIL_URL = "/images/default-groupbuy-thumbnail.jpg"; //예시 경로

    public static GroupBuyCreateResponse from(GroupBuy groupBuy) {
        // 썸네일이 없으면 기본 이미지로 대체
        String thumbnailUrl = (groupBuy.getThumbnailUrl() != null && !groupBuy.getThumbnailUrl().trim().isEmpty())
                ? groupBuy.getThumbnailUrl()
                : DEFAULT_THUMBNAIL_URL;

        List<DiscountStageDto> parsedStages = DiscountStageParser.parseDiscountStages(groupBuy.getDiscountStages());

        return new GroupBuyCreateResponse(
                groupBuy.getId(),
                groupBuy.getTitle(),
                groupBuy.getDescription(),
                thumbnailUrl,
                parsedStages,
                groupBuy.getLimitQuantityPerMember(),
                groupBuy.getStatus(),
                groupBuy.getStartAt(),
                groupBuy.getEndsAt(),
                groupBuy.getCreatedAt(),
                groupBuy.getUpdatedAt(),
                groupBuy.getProduct().getId(),
                groupBuy.getProduct().getName()
        );
    }
}
