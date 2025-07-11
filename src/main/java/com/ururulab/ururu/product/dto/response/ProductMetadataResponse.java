package com.ururulab.ururu.product.dto.response;

import com.ururulab.ururu.product.dto.common.CategoryTreeDto;
import com.ururulab.ururu.product.dto.common.TagDto;

import java.util.List;

public record ProductMetadataResponse(
        List<CategoryTreeDto> categories,
        List<TagDto> tags
) {
}
