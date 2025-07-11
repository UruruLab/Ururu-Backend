package com.ururulab.ururu.product.dto.common;

import java.util.List;

public record CategoryTreeDto(
        Long value,
        String label,
        List<CategoryTreeDto> children
) {
}
