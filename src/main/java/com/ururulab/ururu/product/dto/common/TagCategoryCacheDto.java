package com.ururulab.ururu.product.dto.common;

import com.ururulab.ururu.global.domain.entity.TagCategory;

import java.io.Serializable;

public record TagCategoryCacheDto(
        Long id,
        String name
) implements Serializable {
    public static TagCategoryCacheDto from(TagCategory tagCategory) {
        return new TagCategoryCacheDto(
                tagCategory.getId(),
                tagCategory.getName()
        );
    }
}
