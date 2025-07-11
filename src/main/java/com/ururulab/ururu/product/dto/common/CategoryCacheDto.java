package com.ururulab.ururu.product.dto.common;

import com.ururulab.ururu.product.domain.entity.Category;

import java.io.Serializable;

public record CategoryCacheDto(
        Long id,
        String name
) implements Serializable {
    public static CategoryCacheDto from(Category category) {
        return new CategoryCacheDto(
                category.getId(),
                category.getName()
        );
    }
}
