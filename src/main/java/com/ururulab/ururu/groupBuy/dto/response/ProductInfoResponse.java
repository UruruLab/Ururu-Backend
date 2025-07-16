package com.ururulab.ururu.groupBuy.dto.response;

import com.ururulab.ururu.product.domain.entity.Product;
import com.ururulab.ururu.product.dto.response.ProductNoticeResponse;

import java.util.List;

public record ProductInfoResponse(
        Long id,
        String name,
        String description,
        List<String> categoryIds,
        List<String> tags,
        ProductNoticeResponse productNotice
) {
    public static ProductInfoResponse from(Product product) {
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

        return new ProductInfoResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                categoryIds,
                tags,
                ProductNoticeResponse.from(product.getProductNotice())
        );
    }
}
