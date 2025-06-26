package com.ururulab.ururu.product.domain.dto.response;

import com.ururulab.ururu.product.domain.entity.ProductTag;

import java.time.ZonedDateTime;

public record ProductTagResponse(
        Long id,
        String tagCategoryName,  // TagCategory.name
        ZonedDateTime createdAt,
        ZonedDateTime updatedAt
) {
    public static ProductTagResponse from(ProductTag productTag) {
        return new ProductTagResponse(
                productTag.getId(),
                productTag.getTagCategory().getName(),  // 연관관계를 통해 TagCategory의 name 가져오기
                productTag.getCreatedAt(),
                productTag.getUpdatedAt()
        );
    }
}
