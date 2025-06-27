package com.ururulab.ururu.product.domain.dto.response;

import com.ururulab.ururu.product.domain.entity.Category;

public record CategoryResponse(
        Long id,
        String name,
        Integer depth,
        String path,
        Integer orderIndex
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDepth(),
                category.getPath(),
                category.getOrderIndex()
        );
    }
}
