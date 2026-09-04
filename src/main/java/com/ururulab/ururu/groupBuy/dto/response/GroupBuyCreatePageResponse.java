package com.ururulab.ururu.groupBuy.dto.response;

import com.ururulab.ururu.product.domain.entity.Product;
import com.ururulab.ururu.product.domain.entity.ProductOption;

import java.util.List;

public record GroupBuyCreatePageResponse(
        List<ProductWithOptionsResponse> products
) {
    public static GroupBuyCreatePageResponse from(List<Product> products) {
        List<ProductWithOptionsResponse> productResponses = products.stream()
                .map(ProductWithOptionsResponse::from)
                .toList();

        return new GroupBuyCreatePageResponse(productResponses);
    }

    public record ProductWithOptionsResponse(
            Long productId,
            String productName,
            List<ProductOptionResponse> options
    ) {
        public static ProductWithOptionsResponse from(Product product) {
            List<ProductOptionResponse> options = product.getProductOptions().stream()
                    .filter(option -> !option.getIsDeleted()) // 삭제되지 않은 옵션만
                    .map(ProductOptionResponse::from)
                    .toList();

            return new ProductWithOptionsResponse(
                    product.getId(),
                    product.getName(),
                    options
            );
        }
    }

    public record ProductOptionResponse(
            Long optionId,
            String optionName,
            String optionUrl
    ) {
        public static ProductOptionResponse from(ProductOption option) {
            return new ProductOptionResponse(
                    option.getId(),
                    option.getName(),
                    option.getImageUrl()
            );
        }
    }
}
