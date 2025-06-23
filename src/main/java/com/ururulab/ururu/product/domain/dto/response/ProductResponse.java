package com.ururulab.ururu.product.domain.dto.response;

import com.ururulab.ururu.product.domain.entity.Product;
import com.ururulab.ururu.product.domain.entity.enumerated.Status;

import java.time.ZonedDateTime;
import java.util.List;

public record ProductResponse(
        Long id,
        String name,
        String description,
        Status status,
        ZonedDateTime createdAt,
        ZonedDateTime updatedAt,
        List<CategoryResponse> categories,
        List<ProductOptionResponse> productOptions,
        ProductNoticeResponse productNotice
) {
    public static ProductResponse from(Product product) {
        List<CategoryResponse> categoryResponses = product.getProductCategories().stream()
                .map(pc -> CategoryResponse.from(pc.getCategory()))
                .toList();

        List<ProductOptionResponse> productOptionResponses = product.getProductOptions().stream()
                .filter(po -> !po.isDeleted())
                .map(ProductOptionResponse::from)
                .toList();

        ProductNoticeResponse productNoticeResponse = ProductNoticeResponse.from(product.getProductNotice());

        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getStatus(),
                product.getCreatedAt(),
                product.getUpdatedAt(),
                categoryResponses,
                productOptionResponses,
                productNoticeResponse
        );
    }

    // Service에서 사용
    public static ProductResponse from(
            Product product,
            List<CategoryResponse> categoryResponses,
            List<ProductOptionResponse> productOptionResponses,
            ProductNoticeResponse productNoticeResponse
    ) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getStatus(),
                product.getCreatedAt(),
                product.getUpdatedAt(),
                categoryResponses,
                productOptionResponses,
                productNoticeResponse
        );
    }
}
