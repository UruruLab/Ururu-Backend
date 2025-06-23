package com.ururulab.ururu.product.domain.dto.response;

import com.ururulab.ururu.product.domain.entity.Category;

public record CategoryResponse(
        Long id,
        String name,
        int depth,
        String path,
        int orderIndex
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
