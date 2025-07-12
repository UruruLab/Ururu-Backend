package com.ururulab.ururu.product.event;

import com.ururulab.ururu.product.dto.request.ProductImageUploadRequest;

import java.util.List;

public class ProductImageUploadEvent {

    private final Long productId;
    private final List<ProductImageUploadRequest> uploadRequests; // images → uploadRequests로 명확화

    public ProductImageUploadEvent(Long productId, List<ProductImageUploadRequest> uploadRequests) {
        this.productId = productId;
        this.uploadRequests = uploadRequests;
    }

    public Long getProductId() {
        return productId;
    }

    public List<ProductImageUploadRequest> getUploadRequests() {
        return uploadRequests;
    }
}
