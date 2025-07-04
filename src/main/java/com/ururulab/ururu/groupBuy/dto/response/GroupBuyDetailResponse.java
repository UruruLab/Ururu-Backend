package com.ururulab.ururu.groupBuy.dto.response;

import com.ururulab.ururu.groupBuy.domain.entity.GroupBuy;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyImage;
import com.ururulab.ururu.groupBuy.domain.entity.GroupBuyOption;
import com.ururulab.ururu.groupBuy.domain.entity.enumerated.GroupBuyStatus;
import com.ururulab.ururu.groupBuy.dto.common.DiscountStageDto;
import com.ururulab.ururu.groupBuy.util.DiscountStageParser;
import com.ururulab.ururu.groupBuy.util.TimeCalculator;
import com.ururulab.ururu.product.domain.entity.Product;

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

        // 상품 정보
        ProductInfo product,

        // 옵션 정보
        List<GroupBuyOptionInfo> options,

        // 이미지 정보
        List<GroupBuyImageInfo> images,

        // 메타 정보
        Instant createdAt,
        Instant updatedAt
) {
    public static GroupBuyDetailResponse from(GroupBuy groupBuy,
                                              List<GroupBuyOption> options,
                                              List<GroupBuyImage> images,
                                              Map<Long, Integer> currentStocks) {

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

                ProductInfo.from(groupBuy.getProduct()),

                options.stream()
                        .map(option -> GroupBuyOptionInfo.from(option, currentStocks))
                        .toList(),

                images.stream()
                        .filter(image -> !image.getIsDeleted())
                        .map(GroupBuyImageInfo::from)
                        .toList(),

                groupBuy.getCreatedAt(),
                groupBuy.getUpdatedAt()
        );
    }

    public record ProductInfo(
            Long id,
            String name,
            String description,
            List<String> categoryIds,
            List<String> tags  // 상품 태그 추가
    ) {
        public static ProductInfo from(Product product) {
            List<String> categoryIds = product.getProductCategories().stream()
                    .map(pc -> pc.getCategory().getName())
                    .distinct()
                    .sorted()
                    .toList();

            List<String> tags = product.getProductTags().stream()
                    .map(pt -> pt.getTagCategory().getName()) // ProductTag -> Tag -> name
                    .distinct()
                    .sorted()
                    .toList();

            return new ProductInfo(
                    product.getId(),
                    product.getName(),
                    product.getDescription(),
                    categoryIds,
                    tags
            );
        }
    }

    public record GroupBuyOptionInfo(
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
        public static GroupBuyOptionInfo from(GroupBuyOption option, Map<Long, Integer> currentStocks) {
            Integer currentStock = currentStocks.getOrDefault(option.getId(), option.getStock());
            return new GroupBuyOptionInfo(
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

    public record GroupBuyImageInfo(
            Long id,
            String imageUrl,
            Integer displayOrder
    ) {
        public static GroupBuyImageInfo from(GroupBuyImage image) {
            return new GroupBuyImageInfo(
                    image.getId(),
                    image.getImageUrl(),
                    image.getDisplayOrder()
            );
        }
    }
}
