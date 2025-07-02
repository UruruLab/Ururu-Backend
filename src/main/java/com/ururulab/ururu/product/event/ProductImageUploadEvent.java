package com.ururulab.ururu.product.event;

import com.ururulab.ururu.product.controller.dto.request.ProductImageUploadRequest;

import java.util.List;

public class ProductImageUploadEvent {
    private final Long productId;
    private final List<ProductImageUploadRequest> images;

    public ProductImageUploadEvent(Long productId, List<ProductImageUploadRequest> images) {
        this.productId = productId;
        this.images = images;
    }

    public Long getProductId() {
        return productId;
    }

    public List<ProductImageUploadRequest> getImages() {
        return images;
    }
}
